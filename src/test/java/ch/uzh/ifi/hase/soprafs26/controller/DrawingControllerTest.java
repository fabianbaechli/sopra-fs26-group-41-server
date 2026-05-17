package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateDTO;
import ch.uzh.ifi.hase.soprafs26.service.DrawingService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DrawingController.class)
class DrawingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DrawingService drawingService;

    @Test
    void initializeDrawingSession_validToken_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setToken("valid-token");

        DrawingJoinResponseDTO response = new DrawingJoinResponseDTO();
        response.setSessionId("session-1");
        response.setUserId(1L);
        response.setDrawingState(new DrawingStateDTO());

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(drawingService.initializeDrawingSession(42L, user)).thenReturn(response);

        mockMvc.perform(get("/groups/42/drawing/join")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is("session-1")))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.drawingState.strokes").isArray());

        verify(userService).authenticated("valid-token");
        verify(userService).getUserByToken("valid-token");
        verify(drawingService).initializeDrawingSession(42L, user);
    }

    @Test
    void initializeDrawingSession_invalidToken_returns401() throws Exception {
        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/groups/42/drawing/join")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(userService).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(drawingService, never()).initializeDrawingSession(anyLong(), any());
    }

    @Test
    void initializeDrawingSession_missingAuthorizationHeader_returns400() throws Exception {
        mockMvc.perform(get("/groups/42/drawing/join"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).authenticated(anyString());
        verify(drawingService, never()).initializeDrawingSession(anyLong(), any());
    }

    @Test
    void initializeDrawingSession_malformedAuthorizationHeader_returns400() throws Exception {
        mockMvc.perform(get("/groups/42/drawing/join")
                        .header("Authorization", "valid-token"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).authenticated(anyString());
        verify(drawingService, never()).initializeDrawingSession(anyLong(), any());
    }

    @Test
    void saveGroupDrawing_validMultipartRequest_returns200WithImage() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setToken("valid-token");

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "drawing.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(drawingService.saveDrawingAndCloseSession(eq(42L), eq("session-1"), any(), eq(user)))
                .thenReturn("data:image/png;base64,ZmFrZS1pbWFnZQ==");

        mockMvc.perform(multipart("/groups/42/drawing/save")
                        .file(image)
                        .param("sessionId", "session-1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image", is("data:image/png;base64,ZmFrZS1pbWFnZQ==")));

        verify(userService).authenticated("valid-token");
        verify(userService).getUserByToken("valid-token");
        verify(drawingService).saveDrawingAndCloseSession(eq(42L), eq("session-1"), any(), eq(user));
    }

    @Test
    void saveGroupDrawing_invalidToken_returns401() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "drawing.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );

        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(multipart("/groups/42/drawing/save")
                        .file(image)
                        .param("sessionId", "session-1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(userService).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(drawingService, never()).saveDrawingAndCloseSession(anyLong(), anyString(), any(), any());
    }

    @Test
    void saveGroupDrawing_missingImage_returns400() throws Exception {
        mockMvc.perform(multipart("/groups/42/drawing/save")
                        .param("sessionId", "session-1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest());

        verify(drawingService, never()).saveDrawingAndCloseSession(anyLong(), anyString(), any(), any());
    }
}