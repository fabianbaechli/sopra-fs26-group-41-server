package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.PasswordService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
public class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordService passwordService;

    @Test
    public void createUser_validInput_returns201() throws Exception {
        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setUsername("alice");
        createdUser.setStatus(UserStatus.OFFLINE);

        when(passwordService.hashPassword("plainPassword")).thenReturn("hashedPassword");
        when(userService.createUser(any(User.class))).thenReturn(createdUser);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "plainPassword"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("OFFLINE"));

        verify(passwordService, times(1)).hashPassword("plainPassword");
        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    public void createUser_duplicateUsername_returns409() throws Exception {
        when(passwordService.hashPassword("plainPassword")).thenReturn("hashedPassword");
        when(userService.createUser(any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "The provided username is not unique"));

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "plainPassword"
                                }
                                """))
                .andExpect(status().isConflict());

        verify(passwordService, times(1)).hashPassword("plainPassword");
        verify(userService, times(1)).createUser(any(User.class));
    }
}