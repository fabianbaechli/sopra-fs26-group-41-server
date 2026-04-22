package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.TasteProfile;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsersController.class)
public class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    public void getOwnProfile_validToken_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setToken("valid-token");
        user.setHasLetterboxdData(true);

        TasteProfile tasteProfile = new TasteProfile();
        tasteProfile.setHighlyRatedMovies(3);
        user.setTasteProfile(tasteProfile);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.hasLetterboxdData", is(true)))
                .andExpect(jsonPath("$.stats.moviesLogged", is(0)))
                .andExpect(jsonPath("$.stats.highlyRatedMovies", is(3)));

        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
    }

    @Test
    public void getOwnProfile_invalidToken_returns401() throws Exception {
        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(userService, times(1)).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
    }

    @Test
    public void getOwnProfile_missingAuthorizationHeader_returns400() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).authenticated(anyString());
        verify(userService, never()).getUserByToken(anyString());
    }
}