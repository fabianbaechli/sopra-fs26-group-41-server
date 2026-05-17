package ch.uzh.ifi.hase.soprafs26.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UserHandshakeHandlerTest {

    @Test
    void determineUser_usesPrincipalNameAttribute() {
        TestableUserHandshakeHandler handler = new TestableUserHandshakeHandler();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("principalName", "alice");

        Principal principal = handler.exposeDetermineUser(attributes);

        assertNotNull(principal);
        assertEquals("alice", principal.getName());
    }

    @Test
    void websocketPrincipal_returnsName() {
        WebsocketPrincipal principal = new WebsocketPrincipal("bob");

        assertEquals("bob", principal.getName());
    }

    private static class TestableUserHandshakeHandler extends UserHandshakeHandler {
        Principal exposeDetermineUser(Map<String, Object> attributes) {
            return determineUser(
                    mock(ServerHttpRequest.class),
                    mock(WebSocketHandler.class),
                    attributes
            );
        }
    }
}