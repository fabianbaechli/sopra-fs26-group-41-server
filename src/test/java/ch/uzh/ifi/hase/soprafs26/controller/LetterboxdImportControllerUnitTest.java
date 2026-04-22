package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LetterboxdImportResponse;
import ch.uzh.ifi.hase.soprafs26.service.LetterboxdImportService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LetterboxdImportControllerUnitTest {

    private UserService userService;
    private LetterboxdImportService letterboxdImportService;
    private LetterboxdImportController letterboxdImportController;

    @BeforeEach
    public void setup() {
        userService = Mockito.mock(UserService.class);
        letterboxdImportService = Mockito.mock(LetterboxdImportService.class);
        letterboxdImportController = new LetterboxdImportController(userService, letterboxdImportService);
    }

    @Test
    public void importLetterboxdData_validInput_success() throws IOException {
        String authorization = "Bearer valid-token";

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "letterboxd-export.zip",
                "application/zip",
                "dummy zip content".getBytes()
        );

        LetterboxdImportResponse responseBody = new LetterboxdImportResponse();
        responseBody.setId(1L);
        responseBody.setUsername("alice");
        responseBody.setHasLetterboxdData(true);

        LetterboxdImportResponse.Stats stats = new LetterboxdImportResponse.Stats();
        stats.setMoviesLogged(12);
        stats.setHighlyRatedMovies(3);
        stats.setTopGenres(List.of());
        responseBody.setStats(stats);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(currentUser);
        when(letterboxdImportService.importZip(currentUser, file)).thenReturn(responseBody);

        ResponseEntity<LetterboxdImportResponse> response =
                letterboxdImportController.importLetterboxdData(file, authorization);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("alice", response.getBody().getUsername());
        assertTrue(response.getBody().getHasLetterboxdData());
        assertEquals(12, response.getBody().getStats().getMoviesLogged());
        assertEquals(3, response.getBody().getStats().getHighlyRatedMovies());

        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
        verify(letterboxdImportService, times(1)).importZip(currentUser, file);
    }

    @Test
    public void importLetterboxdData_invalidToken_throwsUnauthorized() throws IOException {
        String authorization = "Bearer invalid-token";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "letterboxd-export.zip",
                "application/zip",
                "dummy zip content".getBytes()
        );

        when(userService.authenticated("invalid-token")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> letterboxdImportController.importLetterboxdData(file, authorization)
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService, times(1)).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(letterboxdImportService, never()).importZip(any(), any());
    }

    @Test
    public void importLetterboxdData_emptyFile_throwsBadRequest() throws IOException {
        String authorization = "Bearer valid-token";

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "letterboxd-export.zip",
                "application/zip",
                new byte[0]
        );

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(currentUser);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> letterboxdImportController.importLetterboxdData(emptyFile, authorization)
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
        verify(letterboxdImportService, never()).importZip(any(), any());
    }

    @Test
    public void importLetterboxdData_nonZipFile_throwsBadRequest() throws IOException {
        String authorization = "Bearer valid-token";

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ratings.csv",
                "text/csv",
                "Name,Rating".getBytes()
        );

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(currentUser);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> letterboxdImportController.importLetterboxdData(file, authorization)
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
        verify(letterboxdImportService, never()).importZip(any(), any());
    }
}