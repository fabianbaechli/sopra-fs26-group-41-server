package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieDetailsResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieTasteOverlapResultDTO;
import ch.uzh.ifi.hase.soprafs26.service.MovieSearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MovieSearchControllerUnitTest {

    private UserService userService;
    private MovieSearchService movieSearchService;
    private MovieSearchController movieSearchController;

    @BeforeEach
    public void setup() {
        userService = Mockito.mock(UserService.class);
        movieSearchService = Mockito.mock(MovieSearchService.class);
        movieSearchController = new MovieSearchController(userService, movieSearchService);
    }

    @Test
    public void getMovieResults_validToken_success() {
        String authorization = "Bearer valid-token";

        MovieSearchResultDTO result = new MovieSearchResultDTO();
        result.setId("tt0111161");
        result.setTitle("The Shawshank Redemption");
        result.setYear(1994);
        result.setPosterUrl("poster-url");

        MovieSearchResponseDTO responseDto = new MovieSearchResponseDTO();
        responseDto.setResults(List.of(result));

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(movieSearchService.searchMovies("shawshank")).thenReturn(responseDto);

        MovieSearchResponseDTO response = movieSearchController.getMovieResults(authorization, "shawshank");

        assertNotNull(response);
        assertNotNull(response.getResults());
        assertEquals(1, response.getResults().size());
        assertEquals("tt0111161", response.getResults().get(0).getId());
        assertEquals("The Shawshank Redemption", response.getResults().get(0).getTitle());

        verify(userService, times(1)).authenticated("valid-token");
        verify(movieSearchService, times(1)).searchMovies("shawshank");
    }

    @Test
    public void getMovieResults_invalidToken_throwsUnauthorized() {
        String authorization = "Bearer invalid-token";

        when(userService.authenticated("invalid-token")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> movieSearchController.getMovieResults(authorization, "shawshank")
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService, times(1)).authenticated("invalid-token");
        verify(movieSearchService, never()).searchMovies(anyString());
    }

    @Test
    public void getMovieDetails_validToken_success() {
        String authorization = "Bearer valid-token";

        MovieDetailsResultDTO responseDto = new MovieDetailsResultDTO();
        responseDto.setId("tt0111161");
        responseDto.setTitle("The Shawshank Redemption");
        responseDto.setYear(1994);
        responseDto.setPosterUrl("poster-url");
        responseDto.setDescription("Two imprisoned men bond over a number of years.");
        responseDto.setDirector("Frank Darabont");
        responseDto.setGenres("Drama");
        responseDto.setRuntime("142 min");
        responseDto.setImdbRating("9.3");

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(movieSearchService.searchMovieDetails("tt0111161")).thenReturn(responseDto);

        MovieDetailsResultDTO response = movieSearchController.getMovieDetails(authorization, "tt0111161");

        assertNotNull(response);
        assertEquals("tt0111161", response.getId());
        assertEquals("The Shawshank Redemption", response.getTitle());
        assertEquals(1994, response.getYear());
        assertEquals("Frank Darabont", response.getDirector());

        verify(userService, times(1)).authenticated("valid-token");
        verify(movieSearchService, times(1)).searchMovieDetails("tt0111161");
    }

    @Test
    public void getMovieDetails_invalidToken_throwsUnauthorized() {
        String authorization = "Bearer invalid-token";

        when(userService.authenticated("invalid-token")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> movieSearchController.getMovieDetails(authorization, "tt0111161")
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService, times(1)).authenticated("invalid-token");
        verify(movieSearchService, never()).searchMovieDetails(anyString());
    }

    @Test
    public void getMovieTasteOverlap_validToken_success() {
        String authorization = "Bearer valid-token";

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        MovieTasteOverlapResultDTO responseDto = new MovieTasteOverlapResultDTO();
        responseDto.setMovieId("tt0111161");
        responseDto.setTasteOverlap(82);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(movieSearchService.getMovieTasteOverlap("tt0111161", user)).thenReturn(responseDto);

        MovieTasteOverlapResultDTO response =
                movieSearchController.getMovieTasteOverlap(authorization, "tt0111161");

        assertNotNull(response);
        assertEquals("tt0111161", response.getMovieId());
        assertEquals(82, response.getTasteOverlap());

        verify(userService, times(1)).authenticated("valid-token");
        verify(userService, times(1)).getUserByToken("valid-token");
        verify(movieSearchService, times(1)).getMovieTasteOverlap("tt0111161", user);
    }

    @Test
    public void getMovieTasteOverlap_invalidToken_throwsUnauthorized() {
        String authorization = "Bearer invalid-token";

        when(userService.authenticated("invalid-token")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> movieSearchController.getMovieTasteOverlap(authorization, "tt0111161")
        );

        assertEquals(401, exception.getStatusCode().value());
        verify(userService, times(1)).authenticated("invalid-token");
        verify(userService, never()).getUserByToken(anyString());
        verify(movieSearchService, never()).getMovieTasteOverlap(anyString(), any(User.class));
    }
}