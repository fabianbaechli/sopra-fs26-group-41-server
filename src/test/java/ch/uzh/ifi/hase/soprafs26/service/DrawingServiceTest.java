package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.AppWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DrawingServiceTest {

    @Mock
    private GroupService groupService;

    @Mock
    private AppWebSocketHandler appWebSocketHandler;

    private DrawingService drawingService;

    private User alice;
    private User bob;
    private Group group;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        drawingService = new DrawingService(
                groupService,
                appWebSocketHandler,
                new ObjectMapper()
        );

        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");

        group = new Group();
        group.setId(42L);
        group.setGroupName("Movie Club");
        group.addMember(alice);
        group.addMember(bob);
    }

    @Test
    void initializeDrawingSession_member_returnsSessionAndSendsStateAndPresence() {
        when(groupService.getGroupById(42L)).thenReturn(group);
        when(groupService.isMember(group, alice)).thenReturn(true);

        DrawingJoinResponseDTO response = drawingService.initializeDrawingSession(42L, alice);

        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertEquals(1L, response.getUserId());
        assertNotNull(response.getDrawingState());
        assertTrue(response.getDrawingState().getStrokes().isEmpty());

        verify(groupService).getGroupById(42L);
        verify(groupService).isMember(group, alice);
        verify(appWebSocketHandler, times(2)).sendToUser(eq("alice"), anyString());
        verify(appWebSocketHandler).sendToUser(eq("alice"), contains("\"event\":\"state\""));
        verify(appWebSocketHandler).sendToUser(eq("alice"), contains("\"event\":\"update\""));
    }

    @Test
    void initializeDrawingSession_secondMember_usesSameSessionAndBroadcastsPresenceToBothUsers() {
        when(groupService.getGroupById(42L)).thenReturn(group);
        when(groupService.isMember(group, alice)).thenReturn(true);
        when(groupService.isMember(group, bob)).thenReturn(true);

        DrawingJoinResponseDTO aliceResponse = drawingService.initializeDrawingSession(42L, alice);
        DrawingJoinResponseDTO bobResponse = drawingService.initializeDrawingSession(42L, bob);

        assertEquals(aliceResponse.getSessionId(), bobResponse.getSessionId());
        assertEquals(2L, bobResponse.getUserId());

        verify(appWebSocketHandler, atLeastOnce()).sendToUser(eq("alice"), contains("\"username\":\"alice\""));
        verify(appWebSocketHandler, atLeastOnce()).sendToUser(eq("alice"), contains("\"username\":\"bob\""));
        verify(appWebSocketHandler, atLeastOnce()).sendToUser(eq("bob"), contains("\"username\":\"alice\""));
        verify(appWebSocketHandler, atLeastOnce()).sendToUser(eq("bob"), contains("\"username\":\"bob\""));
    }

    @Test
    void initializeDrawingSession_nonMember_throwsForbidden() {
        User outsider = new User();
        outsider.setId(99L);
        outsider.setUsername("outsider");

        when(groupService.getGroupById(42L)).thenReturn(group);
        when(groupService.isMember(group, outsider)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingService.initializeDrawingSession(42L, outsider)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(appWebSocketHandler, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void processWebSocketMessage_strokeLifecycle_storesStrokeAndBroadcastsToOtherActiveUsersOnly() {
        when(groupService.getGroupById(42L)).thenReturn(group);
        when(groupService.isMember(group, alice)).thenReturn(true);
        when(groupService.isMember(group, bob)).thenReturn(true);

        DrawingJoinResponseDTO aliceJoin = drawingService.initializeDrawingSession(42L, alice);
        drawingService.initializeDrawingSession(42L, bob);

        String sessionId = aliceJoin.getSessionId();

        reset(appWebSocketHandler);

        drawingService.processWebSocketMessage(
                "alice",
                """
                {
                  "type": "stroke",
                  "event": "start",
                  "sessionId": "%s",
                  "strokeId": "stroke-1",
                  "color": "#000000",
                  "width": 3.0,
                  "point": [10.0, 20.0]
                }
                """.formatted(sessionId)
        );

        verify(appWebSocketHandler).sendToUser(eq("bob"), contains("\"event\":\"started\""));
        verify(appWebSocketHandler, never()).sendToUser(eq("alice"), anyString());

        drawingService.processWebSocketMessage(
                "alice",
                """
                {
                  "type": "stroke",
                  "event": "append",
                  "sessionId": "%s",
                  "strokeId": "stroke-1",
                  "points": [[11.0, 21.0], [12.0, 22.0]]
                }
                """.formatted(sessionId)
        );

        verify(appWebSocketHandler).sendToUser(eq("bob"), contains("\"event\":\"appended\""));

        drawingService.processWebSocketMessage(
                "alice",
                """
                {
                  "type": "stroke",
                  "event": "end",
                  "sessionId": "%s",
                  "strokeId": "stroke-1"
                }
                """.formatted(sessionId)
        );

        verify(appWebSocketHandler).sendToUser(eq("bob"), contains("\"event\":\"ended\""));

        DrawingJoinResponseDTO refreshedState = drawingService.initializeDrawingSession(42L, alice);

        assertEquals(1, refreshedState.getDrawingState().getStrokes().size());
        assertEquals("stroke-1", refreshedState.getDrawingState().getStrokes().get(0).getStrokeId());
        assertEquals(1L, refreshedState.getDrawingState().getStrokes().get(0).getUserId());
        assertEquals("#000000", refreshedState.getDrawingState().getStrokes().get(0).getColor());
        assertEquals(3.0, refreshedState.getDrawingState().getStrokes().get(0).getWidth());
        assertEquals(3, refreshedState.getDrawingState().getStrokes().get(0).getPoints().size());
    }

    @Test
    void processWebSocketMessage_invalidPayloadOrUnknownSession_doesNotBroadcast() {
        drawingService.processWebSocketMessage("alice", "not-json");

        drawingService.processWebSocketMessage(
                "alice",
                """
                {
                  "type": "stroke",
                  "event": "start",
                  "sessionId": "unknown-session",
                  "strokeId": "stroke-1",
                  "color": "#000000",
                  "width": 3.0,
                  "point": [10.0, 20.0]
                }
                """
        );

        verify(appWebSocketHandler, never()).sendToUser(anyString(), anyString());
    }

    @Test
    void saveDrawingAndCloseSession_validMember_savesImageBroadcastsClosureAndRemovesSession() {
        when(groupService.getGroupById(42L)).thenReturn(group);
        when(groupService.isMember(group, alice)).thenReturn(true);

        DrawingJoinResponseDTO joinResponse = drawingService.initializeDrawingSession(42L, alice);
        reset(appWebSocketHandler);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "drawing.png",
                "image/png",
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );

        String responseImage = drawingService.saveDrawingAndCloseSession(
                42L,
                joinResponse.getSessionId(),
                image,
                alice
        );

        assertEquals("data:image/png;base64,ZmFrZS1pbWFnZQ==", responseImage);

        ArgumentCaptor<byte[]> imageCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(groupService).updateProfilePicture(eq(42L), imageCaptor.capture());
        assertArrayEquals("fake-image".getBytes(StandardCharsets.UTF_8), imageCaptor.getValue());

        verify(appWebSocketHandler).sendToUser(eq("alice"), contains("\"event\":\"closed\""));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingService.saveDrawingAndCloseSession(42L, joinResponse.getSessionId(), image, alice)
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void saveDrawingAndCloseSession_nonMember_throwsForbiddenAndDoesNotSaveImage() {
        User outsider = new User();
        outsider.setId(99L);
        outsider.setUsername("outsider");

        when(groupService.getGroupById(42L)).thenReturn(group);
        when(groupService.isMember(group, alice)).thenReturn(true);
        when(groupService.isMember(group, outsider)).thenReturn(false);

        DrawingJoinResponseDTO joinResponse = drawingService.initializeDrawingSession(42L, alice);
        reset(appWebSocketHandler);

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "drawing.png",
                "image/png",
                "fake-image".getBytes(StandardCharsets.UTF_8)
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> drawingService.saveDrawingAndCloseSession(42L, joinResponse.getSessionId(), image, outsider)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(groupService, never()).updateProfilePicture(anyLong(), any());
        verify(appWebSocketHandler, never()).sendToUser(anyString(), anyString());
    }
}