package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LetterboxdImportResponse;
import ch.uzh.ifi.hase.soprafs26.service.LetterboxdImportService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LetterboxdImportController.class)
public class LetterboxdImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private LetterboxdImportService letterboxdImportService;

    @Test
    public void importLetterboxdData_validInput_returns200() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "letterboxd-export.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "dummy zip content".getBytes()
        );

        LetterboxdImportResponse response = new LetterboxdImportResponse();
        response.setId(1L);
        response.setUsername("alice");
        response.setHasLetterboxdData(true);

        LetterboxdImportResponse.Stats stats = new LetterboxdImportResponse.Stats();
        stats.setMoviesLogged(12);
        stats.setHighlyRatedMovies(3);
        stats.setTopGenres(List.of());
        response.setStats(stats);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(currentUser);
        when(letterboxdImportService.importZip(currentUser, file)).thenReturn(response);

        mockMvc.perform(multipart("/import")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.hasLetterboxdData").value(true))
                .andExpect(jsonPath("$.stats.moviesLogged").value(12))
                .andExpect(jsonPath("$.stats.highlyRatedMovies").value(3));

        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
        verify(letterboxdImportService, times(1)).importZip(currentUser, file);
    }

    @Test
    public void importLetterboxdData_invalidToken_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "letterboxd-export.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "dummy zip content".getBytes()
        );

        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(multipart("/import")
                        .file(file)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(userService, times(1)).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(letterboxdImportService, never()).importZip(any(), any());
    }

    @Test
    public void importLetterboxdData_emptyFile_returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "letterboxd-export.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]
        );

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(currentUser);

        mockMvc.perform(multipart("/import")
                        .file(emptyFile)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest());

        verify(letterboxdImportService, never()).importZip(any(), any());
    }

    @Test
    public void importLetterboxdData_nonZipFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ratings.csv",
                "text/csv",
                "Name,Rating".getBytes()
        );

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(currentUser);

        mockMvc.perform(multipart("/import")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest());

        verify(letterboxdImportService, never()).importZip(any(), any());
    }

    @Test
    public void importLetterboxdData_missingAuthorizationHeader_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "letterboxd-export.zip",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "dummy zip content".getBytes()
        );

        mockMvc.perform(multipart("/import")
                        .file(file))
                .andExpect(status().isBadRequest());

        verify(userService, never()).authenticated(anyString());
        verify(letterboxdImportService, never()).importZip(any(), any());
    }
}