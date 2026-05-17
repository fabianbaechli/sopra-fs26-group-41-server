package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthHandshakeInterceptorTest {

    private UserService userService;
    private AuthHandshakeInterceptor interceptor;

    @BeforeEach
    void setup() {
        userService = mock(UserService.class);
        interceptor = new AuthHandshakeInterceptor(userService);
    }

    @Test
    void beforeHandshake_validBearerToken_setsAttributesAndReturnsTrue() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer valid-token");

        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                mock(WebSocketHandler.class),
                attributes
        );

        assertTrue(result);
        assertEquals("alice", attributes.get("principalName"));
        assertEquals(1L, attributes.get("userId"));

        verify(userService).authenticated("valid-token");
        verify(userService).getUserByToken("valid-token");
        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_validTokenQueryParam_setsAttributesAndReturnsTrue() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("token", "query-token");

        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        User user = new User();
        user.setId(2L);
        user.setUsername("bob");

        when(userService.authenticated("query-token")).thenReturn(true);
        when(userService.getUserByToken("query-token")).thenReturn(user);

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                mock(WebSocketHandler.class),
                attributes
        );

        assertTrue(result);
        assertEquals("bob", attributes.get("principalName"));
        assertEquals(2L, attributes.get("userId"));

        verify(userService).authenticated("query-token");
        verify(userService).getUserByToken("query-token");
        verify(response, never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_missingToken_returnsFalseAndUnauthorized() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                mock(WebSocketHandler.class),
                attributes
        );

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(userService, never()).authenticated(anyString());
        verify(userService, never()).getUserByToken(anyString());
    }

    @Test
    void beforeHandshake_invalidToken_returnsFalseAndUnauthorized() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer invalid-token");

        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        when(userService.authenticated("invalid-token")).thenReturn(false);

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                mock(WebSocketHandler.class),
                attributes
        );

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(userService).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_authenticatedButUserMissing_returnsFalseAndUnauthorized() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer valid-token");

        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(null);

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                mock(WebSocketHandler.class),
                attributes
        );

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(userService).authenticated("valid-token");
        verify(userService).getUserByToken("valid-token");
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void afterHandshake_doesNothing() {
        assertDoesNotThrow(() -> interceptor.afterHandshake(
                mock(org.springframework.http.server.ServerHttpRequest.class),
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                null
        ));
    }
}