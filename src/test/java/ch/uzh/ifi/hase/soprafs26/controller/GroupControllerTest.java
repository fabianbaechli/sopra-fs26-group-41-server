package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.FetchedMovie;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.GroupService;
import ch.uzh.ifi.hase.soprafs26.service.PollService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private PollService pollService;

    @Test
    void createGroup_validInput_returns201() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.setOwner(user);
        group.setJoinToken("join-token");
        group.addMember(user);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(groupService.createNewGroup(user, "Movie Night")).thenReturn(group);

        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Movie Night"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is("Movie Night")))
                .andExpect(jsonPath("$.joinUrl", is("/groups/join/join-token")));

        verify(groupService, times(1)).createNewGroup(user, "Movie Night");
    }

    @Test
    void createGroup_invalidToken_returns401() throws Exception {
        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Movie Night"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(groupService, never()).createNewGroup(any(), anyString());
    }

    @Test
    void getUserGroups_validToken_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.addMember(user);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(groupService.getGroupsForUser(user)).thenReturn(List.of(group));

        mockMvc.perform(get("/groups")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].id", is(10)))
                .andExpect(jsonPath("$.groups[0].name", is("Movie Night")))
                .andExpect(jsonPath("$.groups[0].memberCount", is(1)));

        verify(groupService, times(1)).getGroupsForUser(user);
    }

    @Test
    void getGroupDetails_nonMember_returns403() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(groupService.getGroupById(10L)).thenReturn(group);
        when(groupService.isMember(group, user)).thenReturn(false);

        mockMvc.perform(get("/groups/10")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void joinGroup_validToken_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.setJoinToken("join-token");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(groupService.joinGroupByToken("join-token", user)).thenReturn(group);
        when(groupService.recommendMovies(group, 0)).thenReturn(List.of());

        mockMvc.perform(post("/groups/join/join-token")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupUrl", is("join-token")));

        verify(groupService, times(1)).joinGroupByToken("join-token", user);
        verify(groupService, times(1)).recommendMovies(group, 0);
    }

    @Test
    void leaveGroup_validToken_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        mockMvc.perform(post("/groups/10/leave")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("User successfully left the group")));

        verify(groupService, times(1)).leaveGroup(10L, user);
    }

    @Test
    void getGroupRecommendations_validMember_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Group group = new Group();
        group.setId(10L);
        group.setGroupName("Movie Night");
        group.addMember(user);

        FetchedMovie fetchedMovie = new FetchedMovie();
        fetchedMovie.setMovieId("tt0111161");
        fetchedMovie.setName("The Shawshank Redemption");
        fetchedMovie.setPosterUrl("poster-url");
        fetchedMovie.setOverlapScore(0.87);
        group.setRecommendedMovies(List.of(fetchedMovie));

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(groupService.getGroupById(10L)).thenReturn(group);
        when(groupService.isMember(group, user)).thenReturn(true);

        mockMvc.perform(get("/groups/10/recommendations")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId", is(10)))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].movieId", is("tt0111161")))
                .andExpect(jsonPath("$.recommendations[0].title", is("The Shawshank Redemption")))
                .andExpect(jsonPath("$.recommendations[0].posterUrl", is("poster-url")))
                .andExpect(jsonPath("$.recommendations[0].groupMatchScore", is(87)));

        verify(groupService, never()).recommendMovies(any(), anyInt());
    }

    @Test
    void startPoll_validToken_returns204() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        mockMvc.perform(post("/groups/10/poll")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(pollService, times(1)).startPoll(10L, user);
    }
}