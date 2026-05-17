package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.service.DrawingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppWebSocketHandlerTest {

    private DrawingService drawingService;
    private AppWebSocketHandler handler;

    @BeforeEach
    void setup() {
        drawingService = mock(DrawingService.class);
        handler = new AppWebSocketHandler(drawingService);
    }

    @Test
    void afterConnectionEstablished_withPrincipal_registersSessionAndSendToUserWorks() throws Exception {
        WebSocketSession session = session("alice", true);

        handler.afterConnectionEstablished(session);
        handler.sendToUser("alice", "hello");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        assertEquals("hello", captor.getValue().getPayload());
    }

    @Test
    void afterConnectionEstablished_withoutPrincipal_doesNothing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(null);

        handler.afterConnectionEstablished(session);
        handler.sendToUser("alice", "hello");

        verify(session, never()).sendMessage(any());
    }

    @Test
    void handleTextMessage_withPrincipal_delegatesToDrawingService() throws Exception {
        WebSocketSession session = session("alice", true);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"stroke\"}"));

        verify(drawingService).processWebSocketMessage("alice", "{\"type\":\"stroke\"}");
    }

    @Test
    void handleTextMessage_withoutPrincipal_doesNotDelegate() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(null);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"stroke\"}"));

        verify(drawingService, never()).processWebSocketMessage(anyString(), anyString());
    }

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        WebSocketSession session = session("alice", true);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.sendToUser("alice", "after-close");

        verify(session, never()).sendMessage(new TextMessage("after-close"));
    }

    @Test
    void sendToUser_unknownUser_doesNothing() {
        assertDoesNotThrow(() -> handler.sendToUser("nobody", "payload"));
    }

    @Test
    void sendToUser_closedSession_doesNotSend() throws Exception {
        WebSocketSession session = session("alice", false);

        handler.afterConnectionEstablished(session);
        handler.sendToUser("alice", "payload");

        verify(session, never()).sendMessage(any());
    }

    @Test
    void sendToUser_sendThrowsIOException_isSwallowed() throws Exception {
        WebSocketSession session = session("alice", true);
        doThrow(new IOException("connection broken")).when(session).sendMessage(any(TextMessage.class));

        handler.afterConnectionEstablished(session);

        assertDoesNotThrow(() -> handler.sendToUser("alice", "payload"));
        verify(session).sendMessage(any(TextMessage.class));
    }

    private WebSocketSession session(String username, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        Principal principal = () -> username;

        when(session.getPrincipal()).thenReturn(principal);
        when(session.isOpen()).thenReturn(open);

        return session;
    }
}