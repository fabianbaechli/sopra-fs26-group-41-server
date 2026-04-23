package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieDetailsResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollDetailsGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollMovieDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.PollBroadcastService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class PollService {

    private final GroupService groupService;
    private final PollBroadcastService pollBroadcastService;
    private final MovieSearchService movieSearchService; //
    private final Set<Long> activePollGroupIds = ConcurrentHashMap.newKeySet();

    public PollService(GroupService groupService, PollBroadcastService pollBroadcastService,MovieSearchService movieSearchService) {
        this.groupService = groupService;
        this.pollBroadcastService = pollBroadcastService;
        this.movieSearchService = movieSearchService;
    }

    public void startPoll(Long groupId, User user) {
        Group group = groupService.getGroupById(groupId);

        if (!groupService.isMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        if (!activePollGroupIds.add(groupId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another poll is already running");
        }

        try {
            if (group.getRecommendedMovies() == null || group.getRecommendedMovies().isEmpty()) {
                groupService.checkRecommendedMoviesEligibility(group);
                groupService.recommendMovies(group,0); //Temporarily set offset to zero we can start to change this and make it more sensible
            }

            pollBroadcastService.broadcastPollStarted(group);
        } catch (RuntimeException e) {
            activePollGroupIds.remove(groupId);
            throw e;
        }
    }
    public PollDetailsGetDTO getPollDetails(Long groupId, User user) {
        Group group = groupService.getGroupById(groupId);

        if (!groupService.isMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        if (!activePollGroupIds.contains(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active poll exists for this group");
        }

        PollDetailsGetDTO response = new PollDetailsGetDTO();
        response.setGroupId(groupId);
        response.setStatus("OPEN");
        response.setPollCompletedByUser(false);

        // 1. Map the group's current recommended movies to the Poll DTOs
        List<PollMovieDTO> movieDTOs = group.getRecommendedMovies().stream()
                .map(DTOMapper.INSTANCE::convertEntityToPollMovieDTO)
                .toList();

        // 2. Loop through and enrich the data using your existing MovieSearchService
        for (PollMovieDTO dto : movieDTOs) {
            if (dto.getMovieId() != null) {
                try {
                    // Call your existing OMDB fetcher
                    MovieDetailsResultDTO details = movieSearchService.searchMovieDetails(dto.getMovieId());

                    // Populate the missing fields
                    dto.setDescription(details.getDescription());
                    dto.setDirector(details.getDirector());

                    // You might need to cast/parse these depending on how MovieDetailsResultDTO types them
                    if (details.getGenres() != null) {
                        // Splits "Action, Adventure, Sci-Fi" into a proper List
                        List<String> genreList = java.util.Arrays.asList(details.getGenres().split(",\\s*"));
                        dto.setGenres(genreList);
                    }

                    // Parse runtime (usually comes back as "142 min" from OMDB)
                    if (details.getRuntime() != null && details.getRuntime().contains(" min")) {
                        dto.setRuntime(Integer.parseInt(details.getRuntime().replace(" min", "")));
                    }

                    // Parse IMDb rating (usually comes back as a String like "9.3")
                    if (details.getImdbRating() != null && !details.getImdbRating().equals("N/A")) {
                        dto.setImdbRating(Float.parseFloat(details.getImdbRating()));
                    }

                    // Fallback for poster URL if it wasn't saved in the DB
                    if (dto.getPosterUrl() == null) {
                        dto.setPosterUrl(details.getPosterUrl());
                    }

                } catch (Exception e) {
                    // Catch the error so one failing movie doesn't break the whole poll
                    System.err.println("Failed to fetch OMDB details for: " + dto.getTitle());
                }
            }
        }

        response.setMovies(movieDTOs);
        return response;
    }
}
