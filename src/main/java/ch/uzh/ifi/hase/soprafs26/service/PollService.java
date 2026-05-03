package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.FetchedMovie;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieDetailsResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Transactional
public class PollService {

    private final GroupService groupService;
    private final PollBroadcastService pollBroadcastService;
    private final MovieSearchService movieSearchService;
    private final Map<Long, PollState> pollsByGroupId = new ConcurrentHashMap<>();
    private final AtomicLong pollIdSequence = new AtomicLong(1L);

    public PollService(GroupService groupService, PollBroadcastService pollBroadcastService,
            MovieSearchService movieSearchService) {
        this.groupService = groupService;
        this.pollBroadcastService = pollBroadcastService;
        this.movieSearchService = movieSearchService;
    }

    public PollStartResponseDTO startPoll(Long groupId, User user) {
        Group group = getAccessibleGroup(groupId, user);

        PollState existingPoll = pollsByGroupId.get(groupId);
        if (existingPoll != null && "OPEN".equals(existingPoll.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another poll is already running");
        }

        if (group.getRecommendedMovies() == null || group.getRecommendedMovies().isEmpty()) {
            groupService.checkRecommendedMoviesEligibility(group);
            groupService.recommendMovies(group, 0);
        }

        List<FetchedMovie> recommendedMovies = Optional.ofNullable(group.getRecommendedMovies())
                .orElseGet(Collections::emptyList);

        if (recommendedMovies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No recommendations are available for this group");
        }

        PollState pollState = new PollState();
        pollState.pollId = pollIdSequence.getAndIncrement();
        pollState.groupId = groupId;
        pollState.status = "OPEN";
        pollState.movies = recommendedMovies.stream()
                .map(this::toPollMovieDTO)
                .collect(Collectors.toCollection(ArrayList::new));
        pollState.eligibleUserIds = Optional.ofNullable(group.getMembers())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        pollsByGroupId.put(groupId, pollState);
        pollBroadcastService.broadcastPollStarted(group);

        PollStartResponseDTO response = new PollStartResponseDTO();
        response.setGroupId(groupId);
        response.setStatus(pollState.status);
        response.setMovies(copyPollMovies(pollState.movies));
        return response;
    }

    public ActivePollResponseDTO getActivePoll(Long groupId, User user) {
        Group group = getAccessibleGroup(groupId, user);
        PollState pollState = getOpenPoll(groupId);

        ActivePollResponseDTO response = new ActivePollResponseDTO();
        response.setGroupId(groupId);
        response.setStatus(pollState.status);
        response.setPollCompletedByUser(pollState.votesByUserId.containsKey(user.getId()));
        response.setMovies(buildDetailedPollMovies(group, pollState));
        return response;
    }

    public PollVoteResponseDTO submitVotes(Long groupId, User user, PollVoteSubmissionDTO submission) {
        Group group = getAccessibleGroup(groupId, user);
        PollState pollState = getOpenPoll(groupId);

        if (pollState.votesByUserId.containsKey(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has already voted");
        }

        List<PollVoteItemDTO> votes = submission == null || submission.getVotes() == null
                ? Collections.emptyList()
                : submission.getVotes();

        validateVotes(pollState, votes);

        Map<String, Boolean> normalizedVotes = new LinkedHashMap<>();
        for (PollVoteItemDTO vote : votes) {
            normalizedVotes.put(vote.getMovieId(), vote.isInterested());
        }
        pollState.votesByUserId.put(user.getId(), normalizedVotes);

        if (isPollFinished(pollState)) {
            finishPoll(group, pollState);
        }

        PollVoteResponseDTO response = new PollVoteResponseDTO();
        response.setUserId(user.getId());
        return response;
    }

    public PollResultsResponseDTO getPollResults(Long groupId, User user) {
        getAccessibleGroup(groupId, user);

        PollState pollState = pollsByGroupId.get(groupId);
        if (pollState == null || !"FINISHED".equals(pollState.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Poll has not finished yet");
        }

        PollResultsResponseDTO response = new PollResultsResponseDTO();
        response.setStatus(pollState.status);
        response.setTopMovies(copyPollMovies(pollState.topMovies));
        return response;
    }

    private Group getAccessibleGroup(Long groupId, User user) {
        Group group = groupService.getGroupById(groupId);
        if (!groupService.isMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }
        return group;
    }

    private PollState getOpenPoll(Long groupId) {
        PollState pollState = pollsByGroupId.get(groupId);
        if (pollState == null || !"OPEN".equals(pollState.status)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active poll exists for this group");
        }
        return pollState;
    }

    private void validateVotes(PollState pollState, List<PollVoteItemDTO> votes) {
        Set<String> validMovieIds = pollState.movies.stream()
                .map(PollMovieDTO::getMovieId)
                .collect(Collectors.toSet());

        Set<String> seenMovieIds = new HashSet<>();
        for (PollVoteItemDTO vote : votes) {
            if (vote.getMovieId() == null || !validMovieIds.contains(vote.getMovieId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votes contain an invalid movieId");
            }
            if (!seenMovieIds.add(vote.getMovieId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votes contain duplicate movieIds");
            }
        }
    }

    private boolean isPollFinished(PollState pollState) {
        return !pollState.eligibleUserIds.isEmpty()
                && pollState.votesByUserId.keySet().containsAll(pollState.eligibleUserIds);
    }

    private void finishPoll(Group group, PollState pollState) {
        Map<String, Long> interestedCounts = new LinkedHashMap<>();
        for (PollMovieDTO movie : pollState.movies) {
            interestedCounts.put(movie.getMovieId(), 0L);
        }

        for (Map<String, Boolean> userVotes : pollState.votesByUserId.values()) {
            for (Map.Entry<String, Boolean> entry : userVotes.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    interestedCounts.computeIfPresent(entry.getKey(), (movieId, count) -> count + 1);
                }
            }
        }

        long highestScore = interestedCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L);


        List<PollMovieDTO> winners = pollState.movies.stream()
                .filter(movie -> interestedCounts.getOrDefault(movie.getMovieId(), 0L) == highestScore)
                .map(movie -> {
                    PollMovieDTO copy = copyPollMovie(movie);
                    copy.setVotes((int) highestScore); // Set the calculated votes
                    return copy;
                })
                .toList();

        pollState.status = "FINISHED";
        pollState.topMovies = winners;

        PollMovieDTO representativeWinner = winners.isEmpty() ? null : winners.get(0);
        pollBroadcastService.broadcastPollFinished(group, pollState.pollId, representativeWinner);
    }

    private List<ActivePollMovieDTO> buildDetailedPollMovies(Group group, PollState pollState) {
        return pollState.movies.stream()
                .map(movie -> enrichPollMovie(movie, group))
                .toList();
    }

    private ActivePollMovieDTO enrichPollMovie(PollMovieDTO movie, Group group) {
        ActivePollMovieDTO dto = new ActivePollMovieDTO();
        dto.setMovieId(movie.getMovieId());
        dto.setTitle(movie.getTitle());
        dto.setPosterUrl(movie.getPosterUrl());

        try {
            MovieDetailsResultDTO details = movieSearchService.searchMovieDetails(movie.getMovieId());
            dto.setDescription(details.getDescription());
            dto.setDirector(details.getDirector());
            dto.setGenres(details.getGenres());
            dto.setRuntime(details.getRuntime());
            dto.setImdbRating(details.getImdbRating());
            if (details.getPosterUrl() != null) {
                dto.setPosterUrl(details.getPosterUrl());
            }
        } catch (RuntimeException ignored) {
            // Return the basic movie information even if the details lookup fails.
        }

        return dto;
    }

    private PollMovieDTO toPollMovieDTO(FetchedMovie fetchedMovie) {
        PollMovieDTO dto = new PollMovieDTO();
        dto.setMovieId(fetchedMovie.getMovieId());
        dto.setTitle(fetchedMovie.getName());
        dto.setPosterUrl(fetchedMovie.getPosterUrl());
        return dto;
    }

    private List<PollMovieDTO> copyPollMovies(List<PollMovieDTO> movies) {
        return movies == null ? Collections.emptyList() : movies.stream().map(this::copyPollMovie).toList();
    }

    private PollMovieDTO copyPollMovie(PollMovieDTO movie) {
        PollMovieDTO copy = new PollMovieDTO();
        copy.setMovieId(movie.getMovieId());
        copy.setTitle(movie.getTitle());
        copy.setPosterUrl(movie.getPosterUrl());
        copy.setVotes(movie.getVotes()); // Make sure votes are transferred
        return copy;
    }

    private static class PollState {
        private Long pollId;
        private Long groupId;
        private String status;
        private List<PollMovieDTO> movies = new ArrayList<>();
        private Set<Long> eligibleUserIds = new LinkedHashSet<>();
        private Map<Long, Map<String, Boolean>> votesByUserId = new LinkedHashMap<>();
        private List<PollMovieDTO> topMovies = new ArrayList<>();
    }
}
