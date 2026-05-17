package ch.uzh.ifi.hase.soprafs26.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionAdviceTest {

    private final GlobalExceptionAdvice advice = new GlobalExceptionAdvice();

    @Test
    void handleConflict_illegalArgument_returns409() {
        ResponseEntity<Object> response = advice.handleConflict(
                new IllegalArgumentException("bad input"),
                new ServletWebRequest(new MockHttpServletRequest())
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This should be application specific", response.getBody());
    }

    @Test
    void handleConflict_illegalState_returns409() {
        ResponseEntity<Object> response = advice.handleConflict(
                new IllegalStateException("bad state"),
                new ServletWebRequest(new MockHttpServletRequest())
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleTransactionSystemException_returnsConflictResponseStatusException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");

        ResponseStatusException exception = advice.handleTransactionSystemException(
                new TransactionSystemException("transaction failed"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("transaction failed", exception.getReason());
    }

    @Test
    void handleInternalServerError_returns500ResponseStatusException() {
        HttpServerErrorException input =
                HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "server error",
                        null,
                        null,
                        null
                );

        ResponseStatusException exception = advice.handleException(input);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }
}