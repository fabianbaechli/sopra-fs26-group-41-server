package ch.uzh.ifi.hase.soprafs26.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AppWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> sessionsByUsername = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (session.getPrincipal() == null) {
            return;
        }

        String username = session.getPrincipal().getName();
        sessionsByUsername
                .computeIfAbsent(username, key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Incoming messages handled here in the future
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (session.getPrincipal() == null) {
            return;
        }

        String username = session.getPrincipal().getName();
        Set<WebSocketSession> sessions = sessionsByUsername.get(username);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByUsername.remove(username);
            }
        }
    }

    public void sendToUser(String username, String payload) {
        Set<WebSocketSession> sessions = sessionsByUsername.get(username);
        if (sessions == null) {
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException ignored) {
                }
            }
        }
    }
}