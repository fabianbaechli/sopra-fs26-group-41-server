package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.PasswordService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordService passwordService;

    @Test
    void login_validInput_returnsAuthenticatedUser() throws Exception {
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
        updatedUser.setStatus(UserStatus.OFFLINE);

        given(userService.getUserByUsername("alice")).willReturn(storedUser);
        given(passwordService.matches("plainPassword", "hashedPassword")).willReturn(true);
        given(userService.newToken()).willReturn("new-token");
        given(userService.updateUserToken(storedUser, "new-token")).willReturn(updatedUser);

        doAnswer(invocation -> {
            User userArg = invocation.getArgument(0);
            userArg.setStatus(UserStatus.ONLINE);
            return null;
        }).when(userService).updateUserStatus(updatedUser, UserStatus.ONLINE);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "plainPassword"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token").value("new-token"));

        verify(userService).getUserByUsername("alice");
        verify(passwordService).matches("plainPassword", "hashedPassword");
        verify(userService).newToken();
        verify(userService).updateUserToken(storedUser, "new-token");
        verify(userService).updateUserStatus(updatedUser, UserStatus.ONLINE);
    }

    @Test
    void login_missingPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUserByUsername(any());
        verify(passwordService, never()).matches(any(), any());
    }

    @Test
    void login_unknownUsername_returnsUnauthorized() throws Exception {
        given(userService.getUserByUsername("alice")).willReturn(null);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "plainPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(userService).getUserByUsername("alice");
        verify(passwordService, never()).matches(any(), any());
        verify(userService, never()).newToken();
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        User storedUser = new User();
        storedUser.setId(1L);
        storedUser.setUsername("alice");
        storedUser.setPassword("hashedPassword");

        given(userService.getUserByUsername("alice")).willReturn(storedUser);
        given(passwordService.matches("wrongPassword", "hashedPassword")).willReturn(false);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "wrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(userService).getUserByUsername("alice");
        verify(passwordService).matches("wrongPassword", "hashedPassword");
        verify(userService, never()).newToken();
        verify(userService, never()).updateUserToken(any(), any());
        verify(userService, never()).updateUserStatus(any(), eq(UserStatus.ONLINE));
    }
}