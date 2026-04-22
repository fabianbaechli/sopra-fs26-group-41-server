package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.AuthenticationGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.AuthenticationPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.PasswordService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthenticationControllerUnitTest {

    private UserService userService;
    private PasswordService passwordService;
    private AuthenticationController authenticationController;

    @BeforeEach
    public void setup() {
        userService = Mockito.mock(UserService.class);
        passwordService = Mockito.mock(PasswordService.class);
        authenticationController = new AuthenticationController(userService, passwordService);
    }

    @Test
    public void login_validInput_success() {
        AuthenticationPostDTO request = new AuthenticationPostDTO();
        request.setUsername("alice");
        request.setPassword("plainPassword");

        User storedUser = new User();
        storedUser.setId(1L);
        storedUser.setUsername("alice");
        storedUser.setPassword("hashedPassword");
        storedUser.setStatus(UserStatus.OFFLINE);

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("alice");
        updatedUser.setPassword("hashedPassword");
        updatedUser.setToken("new-token");
        updatedUser.setStatus(UserStatus.ONLINE);

        when(userService.getUserByUsername("alice")).thenReturn(storedUser);
        when(passwordService.matches("plainPassword", "hashedPassword")).thenReturn(true);
        when(userService.newToken()).thenReturn("new-token");
        when(userService.updateUserToken(storedUser, "new-token")).thenReturn(updatedUser);

        AuthenticationGetDTO response = authenticationController.login(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("alice", response.getUsername());
        assertEquals("new-token", response.getToken());

        verify(userService, times(1)).getUserByUsername("alice");
        verify(passwordService, times(1)).matches("plainPassword", "hashedPassword");
        verify(userService, times(1)).newToken();
        verify(userService, times(1)).updateUserToken(storedUser, "new-token");
        verify(userService, times(1)).updateUserStatus(updatedUser, UserStatus.ONLINE);
    }

    @Test
    public void login_missingPassword_throwsBadRequest() {
        AuthenticationPostDTO request = new AuthenticationPostDTO();
        request.setUsername("alice");
        request.setPassword(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authenticationController.login(request)
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(userService, never()).getUserByUsername(anyString());
        verify(passwordService, never()).matches(anyString(), anyString());
    }

    @Test
    public void login_unknownUsername_throwsUnauthorized() {
        AuthenticationPostDTO request = new AuthenticationPostDTO();
        request.setUsername("alice");
        request.setPassword("plainPassword");

        when(userService.getUserByUsername("alice")).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authenticationController.login(request)
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService, times(1)).getUserByUsername("alice");
        verify(passwordService, never()).matches(anyString(), anyString());
        verify(userService, never()).newToken();
    }

    @Test
    public void login_wrongPassword_throwsUnauthorized() {
        AuthenticationPostDTO request = new AuthenticationPostDTO();
        request.setUsername("alice");
        request.setPassword("wrongPassword");

        User storedUser = new User();
        storedUser.setId(1L);
        storedUser.setUsername("alice");
        storedUser.setPassword("hashedPassword");

        when(userService.getUserByUsername("alice")).thenReturn(storedUser);
        when(passwordService.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authenticationController.login(request)
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService, times(1)).getUserByUsername("alice");
        verify(passwordService, times(1)).matches("wrongPassword", "hashedPassword");
        verify(userService, never()).newToken();
        verify(userService, never()).updateUserToken(any(User.class), anyString());
        verify(userService, never()).updateUserStatus(any(User.class), eq(UserStatus.ONLINE));
    }
}