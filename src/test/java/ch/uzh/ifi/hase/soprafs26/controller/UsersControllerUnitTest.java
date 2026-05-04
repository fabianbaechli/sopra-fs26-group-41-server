package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.TasteProfile;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UsersGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.MovieSearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UsersControllerUnitTest {

    private UserService userService;
    private UsersController usersController;
    private MovieSearchService movieSearchService;

    @BeforeEach
    public void setup() {
        userService = Mockito.mock(UserService.class);
        usersController = new UsersController(userService, movieSearchService);
    }

    @Test
    public void getOwnProfile_validToken_success() {
        String header = "Bearer valid-token";

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

        UsersGetDTO response = usersController.getOwnProfile(header);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("alice", response.getUsername());
        assertTrue(response.getHasLetterboxdData());

        assertNotNull(response.getStats());
        assertEquals(0, response.getStats().getMoviesLogged());
        assertEquals(3, response.getStats().getHighlyRatedMovies());

        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
    }

    @Test
    public void getOwnProfile_invalidToken_throwsUnauthorized() {
        String header = "Bearer invalid-token";

        when(userService.authenticated("invalid-token")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usersController.getOwnProfile(header)
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService, times(1)).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
    }
}