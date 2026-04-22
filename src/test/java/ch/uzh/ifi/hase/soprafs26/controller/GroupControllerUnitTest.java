package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.FetchedMovie;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupCreatePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupCreateResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupRecommendationDTO;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.PollService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GroupControllerUnitTest {

    private UserService userService;
    private GroupService groupService;
    private PollService pollService;
    private GroupController groupController;

    @BeforeEach
    public void setup() {
        userService = Mockito.mock(UserService.class);
        groupService = Mockito.mock(GroupService.class);
        pollService = Mockito.mock(PollService.class);
        groupController = new GroupController(userService, groupService, pollService);
    }

    @Test
    public void createGroup_validToken_success() {
        String authorization = "Bearer valid-token";

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.setOwner(user);
        group.setJoinToken("join-token");
        group.addMember(user);

        GroupCreatePostDTO request = new GroupCreatePostDTO();
        request.setName("Movie Night");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(groupService.createNewGroup(user, "Movie Night")).thenReturn(group);

        GroupCreateResponseDTO response = groupController.createGroup(authorization, request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Movie Night", response.getName());
        assertEquals("/groups/join/join-token", response.getJoinUrl());

        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
        verify(groupService, times(1)).createNewGroup(user, "Movie Night");
    }

    @Test
    public void getGroupDetails_nonMember_throwsForbidden() {
        String authorization = "Bearer valid-token";

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.setJoinToken("join-token");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(currentUser);
        when(groupService.getGroupById(10L)).thenReturn(group);
        when(groupService.isMember(group, currentUser)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> groupController.getGroupDetails(10L, authorization)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(groupService, times(1)).getGroupById(10L);
        verify(groupService, times(1)).isMember(group, currentUser);
    }

    @Test
    public void getGroupRecommendations_emptyCache_fetchesRecommendations() {
        String authorization = "Bearer valid-token";

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.addMember(user);
        group.setRecommendedMovies(new ArrayList<>());

        FetchedMovie fetchedMovie = new FetchedMovie();
        fetchedMovie.setMovieId("tt0111161");
        fetchedMovie.setName("The Shawshank Redemption");
        fetchedMovie.setPosterUrl("poster-url");
        fetchedMovie.setOverlapScore(0.87);

        List<FetchedMovie> recommendedMovies = List.of(fetchedMovie);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(groupService.getGroupById(10L)).thenReturn(group);
        when(groupService.isMember(group, user)).thenReturn(true);
        when(groupService.recommendMovies(group, 0)).thenReturn(recommendedMovies);

        GroupRecommendationDTO response = groupController.getGroupRecommendations(10L, authorization, 0);

        assertNotNull(response);
        assertEquals(10L, response.getGroupId());
        assertNotNull(response.getRecommendations());
        assertEquals(1, response.getRecommendations().size());
        assertEquals("tt0111161", response.getRecommendations().get(0).getMovieId());
        assertEquals("The Shawshank Redemption", response.getRecommendations().get(0).getTitle());
        assertEquals("poster-url", response.getRecommendations().get(0).getPosterUrl());
        assertEquals(87, response.getRecommendations().get(0).getGroupMatchScore());

        verify(groupService, times(1)).checkRecommendedMoviesEligibility(group);
        verify(groupService, times(1)).recommendMovies(group, 0);
    }

    @Test
    public void startPoll_validToken_callsPollService() {
        String authorization = "Bearer valid-token";

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        groupController.startPoll(10L, authorization);

        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
        verify(pollService, times(1)).startPoll(10L, user);
    }
}