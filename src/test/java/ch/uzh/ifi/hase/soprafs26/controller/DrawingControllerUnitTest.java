package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateDTO;
import ch.uzh.ifi.hase.soprafs26.service.DrawingService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DrawingControllerUnitTest {

    private UserService userService;
    private DrawingService drawingService;
    private DrawingController drawingController;

    private User user;

    @BeforeEach
    void setup() {
        userService = Mockito.mock(UserService.class);
        drawingService = Mockito.mock(DrawingService.class);
        drawingController = new DrawingController(userService, drawingService);

        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setToken("valid-token");
    }

    @Test
    void initializeDrawingSession_validToken_returnsServiceResponse() {
        DrawingJoinResponseDTO responseDTO = new DrawingJoinResponseDTO();
        responseDTO.setSessionId("session-1");
        responseDTO.setUserId(1L);
        responseDTO.setDrawingState(new DrawingStateDTO());

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(drawingService.initializeDrawingSession(42L, user)).thenReturn(responseDTO);

        DrawingJoinResponseDTO response = drawingController.initializeDrawingSession(42L, "Bearer valid-token");

        assertSame(responseDTO, response);
        verify(userService).authenticated("valid-token");
        verify(userService).getUserByToken("valid-token");
        verify(drawingService).initializeDrawingSession(42L, user);
    }

    @Test
    void initializeDrawingSession_missingBearerToken_throwsBadRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingController.initializeDrawingSession(42L, "valid-token")
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(userService, never()).authenticated(anyString());
        verify(drawingService, never()).initializeDrawingSession(anyLong(), any());
    }

    @Test
    void initializeDrawingSession_invalidToken_throwsUnauthorized() {
        when(userService.authenticated("invalid-token")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingController.initializeDrawingSession(42L, "Bearer invalid-token")
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(drawingService, never()).initializeDrawingSession(anyLong(), any());
    }

    @Test
    void saveGroupDrawing_validToken_returnsImageMap() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "drawing.png",
                "image/png",
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(drawingService.saveDrawingAndCloseSession(42L, "session-1", image, user))
                .thenReturn("data:image/png;base64,ZmFrZS1pbWFnZQ==");

        Map<String, String> response = drawingController.saveGroupDrawing(
                42L,
                "session-1",
                image,
                "Bearer valid-token"
        );

        assertEquals("data:image/png;base64,ZmFrZS1pbWFnZQ==", response.get("image"));
        verify(userService).authenticated("valid-token");
        verify(userService).getUserByToken("valid-token");
        verify(drawingService).saveDrawingAndCloseSession(42L, "session-1", image, user);
    }

    @Test
    void saveGroupDrawing_invalidToken_throwsUnauthorized() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "drawing.png",
                "image/png",
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );

        when(userService.authenticated("invalid-token")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingController.saveGroupDrawing(42L, "session-1", image, "Bearer invalid-token")
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(drawingService, never()).saveDrawingAndCloseSession(anyLong(), anyString(), any(), any());
    }
}