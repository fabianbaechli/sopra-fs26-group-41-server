package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecommendResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class GroupService {

    private final Logger log = LoggerFactory.getLogger(GroupService.class);
    private final GroupRepository groupRepository;
    private final MovieSearchService movieSearchService;

    @Autowired
    public GroupService(GroupRepository groupRepository, MovieSearchService movieSearchService) {
        this.groupRepository = groupRepository;
        this.movieSearchService = movieSearchService;
    }

    public Group createNewGroup(User user, String name) {
        if (!groupNameUnique(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The groupname is already taken");
        }
        Group group = new Group();
        group.setGroupName(name);
        group.setOwner(user);
        group.setJoinToken(UUID.randomUUID().toString());
        group.addMember(user);

        return groupRepository.save(group);
    }

    public Group joinGroupByToken(String joinToken, User user) {
        log.debug("Attempting to join group with token: {} for user: {}", joinToken, user.getUsername());

        Group group = groupRepository.findGroupByJoinToken(joinToken);

        if (group == null) {
            log.error("Failed to join group. Invalid join token: {}", joinToken);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join token");
        }

        if (group.getMembers() != null && group.getMembers().contains(user)) {
            log.warn("User {} attempted to join group {} but is already a member.", user.getUsername(), group.getGroupName());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is already a member. Group URL: " + group.getJoinToken());
        }

        // Add user to the group
        group.addMember(user);
        List<TasteProfile> tasteProfiles = new ArrayList<>();
        for (User member : group.getMembers()) {
            tasteProfiles.add(member.getTasteProfile());
        }
        group.setGroupTasteProfile(TasteProfile.MergeTasteProfiles(tasteProfiles)); //  this should work and merge the tasteprofiles there surely is a more efficient way but we ball
        log.info("Successfully added user {} to group {}", user.getUsername(), group.getGroupName());

        // Save and return the updated group
        return groupRepository.save(group);
    }

    public List<Group> getGroupsForUser(User user) {
        return groupRepository.findByMembersContaining(user);
    }

    private boolean groupNameUnique(String name) {
        return groupRepository.findGroupByGroupName(name) == null;
    }

    public Group getGroupById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    public boolean isMember(Group group, User user) {
        return group.getMembers() != null && group.getMembers().contains(user);
    }

    public List<FetchedMovie> recommendMovies(Group group) {
        if (group.getMembers() == null || group.getMembers().isEmpty()) {
            return new ArrayList<>(); //
        }

        // 1. Gather all TasteProfiles from members
        List<TasteProfile> memberProfiles = group.getMembers().stream()
                .map(User::getTasteProfile)
                .filter(Objects::nonNull)
                .toList();

        // 2. Merge them to get a master list of all rated movies in the group
        TasteProfile mergedProfile = TasteProfile.MergeTasteProfiles(memberProfiles);

        // 3. Extract just the internal microservice IDs
        List<String> watchedIds = mergedProfile.getRatedMovies().stream()
                .map(RatedMovie::getMovieId) // Assuming RatedMovie inherits/has getMovieId()
                .filter(id -> id != null && !id.isEmpty())
                .toList();

        // 4. Call the microservice (e.g., get top 10 movies, 0 offset)
        List<RecommendResponseDTO> recommendations = movieSearchService.fetchRecommendations(watchedIds, 10, 0);

        // 5. Convert DTOs to FetchedMovie entities
        List<FetchedMovie> fetchedMovies = new ArrayList<>();
        for (RecommendResponseDTO rec : recommendations) {
            FetchedMovie movie = new FetchedMovie();
            movie.setInternalId(rec.getMovie_id());

            String title = rec.getTitle() != null ? rec.getTitle() : "Unknown Title";
            movie.setName(title);
            movie.setOverlapScore(rec.getOverlap_score());

            // --- New OMDB Fetch Logic ---
            if (!"Unknown Title".equals(title)) {
                try {
                    // Search OMDB by title
                    MovieSearchResponseDTO searchResponse = movieSearchService.searchMovies(title);

                    // If we get results, take the IMDB ID from the first match
                    if (searchResponse != null && searchResponse.getResults() != null && !searchResponse.getResults().isEmpty()) {
                        movie.setMovieId(searchResponse.getResults().get(0).getId());
                        movie.setPosterUrl(searchResponse.getResults().get(0).getPosterUrl());
                    }
                } catch (Exception e) {
                    // Log but don't crash the whole recommendation process if one movie fails
                    log.warn("Failed to fetch IMDB ID from OMDB for title: {}", title);
                }
            }

            fetchedMovies.add(movie);
        }

        // 6. Save back to the group
        group.setRecommendedMovies(fetchedMovies);
        groupRepository.save(group);
        return  fetchedMovies;
    }
    public void checkRecommendedMoviesEligibility(Group group) {
        boolean isEligible = group.getMembers().stream()
                .anyMatch(u -> !u.getTasteProfile().getRatedMovies().isEmpty());

        if (!isEligible) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No Members have uploaded a Taste Profile");
        }
    }

    public void leaveGroup(Long groupId, User user) {
        log.debug("User {} attempting to leave group {}", user.getUsername(), groupId);

        // Fetch group or throw 404 if it doesn't exist
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // Failure: User is not a member (403 Forbidden)
        if (group.getMembers() == null || !group.getMembers().contains(user)) {
            log.warn("User {} tried to leave group {} but is not a member.", user.getUsername(), groupId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of the group");
        }

        // Remove user and save
        group.removeMember(user);
        log.info("User {} successfully left group {}", user.getUsername(), groupId);

        groupRepository.save(group);
    }
}