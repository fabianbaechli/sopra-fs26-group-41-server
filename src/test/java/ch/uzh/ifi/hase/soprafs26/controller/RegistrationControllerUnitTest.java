package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.PasswordService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RegistrationControllerUnitTest {

    private UserService userService;
    private PasswordService passwordService;
    private RegistrationController registrationController;

    @BeforeEach
    public void setup() {
        userService = Mockito.mock(UserService.class);
        passwordService = Mockito.mock(PasswordService.class);
        registrationController = new RegistrationController(userService, passwordService);
    }

    @Test
    public void createUser_validInput_success() {
        UserPostDTO request = new UserPostDTO();
        request.setUsername("alice");
        request.setPassword("plainPassword");

        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setUsername("alice");
        createdUser.setStatus(UserStatus.OFFLINE);

        when(passwordService.hashPassword("plainPassword")).thenReturn("hashedPassword");
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        UserGetDTO response = registrationController.createUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("alice", response.getUsername());
        assertEquals(UserStatus.OFFLINE, response.getStatus());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService, times(1)).createUser(userCaptor.capture());

        User passedUser = userCaptor.getValue();
        assertEquals("alice", passedUser.getUsername());
        assertEquals("hashedPassword", passedUser.getPassword());

        verify(passwordService, times(1)).hashPassword("plainPassword");
    }

    @Test
    public void createUser_duplicateUsername_throwsConflict() {
        UserPostDTO request = new UserPostDTO();
        request.setUsername("alice");
        request.setPassword("plainPassword");

        when(passwordService.hashPassword("plainPassword")).thenReturn("hashedPassword");
        when(userService.createUser(any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "The provided username is not unique"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> registrationController.createUser(request)
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(passwordService, times(1)).hashPassword("plainPassword");
        verify(userService, times(1)).createUser(any(User.class));
    }
}