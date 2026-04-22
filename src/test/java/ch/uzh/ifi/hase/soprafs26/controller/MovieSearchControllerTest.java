package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieDetailsResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieTasteOverlapResultDTO;
import ch.uzh.ifi.hase.soprafs26.service.MovieSearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieSearchController.class)
public class MovieSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MovieSearchService movieSearchService;

    @Test
    public void getMovieResults_validToken_returns200() throws Exception {
        MovieSearchResultDTO result = new MovieSearchResultDTO();
        result.setId("tt0111161");
        result.setTitle("The Shawshank Redemption");
        result.setYear(1994);
        result.setPosterUrl("poster-url");

        MovieSearchResponseDTO responseDto = new MovieSearchResponseDTO();
        responseDto.setResults(List.of(result));

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(movieSearchService.searchMovies("shawshank")).thenReturn(responseDto);

        mockMvc.perform(get("/movies/search")
                        .header("Authorization", "Bearer valid-token")
                        .param("query", "shawshank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].id", is("tt0111161")))
                .andExpect(jsonPath("$.results[0].title", is("The Shawshank Redemption")))
                .andExpect(jsonPath("$.results[0].year", is(1994)))
                .andExpect(jsonPath("$.results[0].posterUrl", is("poster-url")));

        verify(movieSearchService, times(1)).searchMovies("shawshank");
    }

    @Test
    public void getMovieResults_invalidToken_returns401() throws Exception {
        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/movies/search")
                        .header("Authorization", "Bearer invalid-token")
                        .param("query", "shawshank"))
                .andExpect(status().isUnauthorized());

        verify(movieSearchService, never()).searchMovies(anyString());
    }

    @Test
    public void getMovieResults_missingAuthorizationHeader_returns400() throws Exception {
        mockMvc.perform(get("/movies/search")
                        .param("query", "shawshank"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).authenticated(anyString());
        verify(movieSearchService, never()).searchMovies(anyString());
    }

    @Test
    public void getMovieDetails_validToken_returns200() throws Exception {
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

        mockMvc.perform(get("/movies/tt0111161")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("tt0111161")))
                .andExpect(jsonPath("$.title", is("The Shawshank Redemption")))
                .andExpect(jsonPath("$.year", is(1994)))
                .andExpect(jsonPath("$.posterUrl", is("poster-url")))
                .andExpect(jsonPath("$.director", is("Frank Darabont")))
                .andExpect(jsonPath("$.genres", is("Drama")))
                .andExpect(jsonPath("$.runtime", is("142 min")))
                .andExpect(jsonPath("$.imdbRating", is("9.3")));

        verify(movieSearchService, times(1)).searchMovieDetails("tt0111161");
    }

    @Test
    public void getMovieDetails_invalidToken_returns401() throws Exception {
        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/movies/tt0111161")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(movieSearchService, never()).searchMovieDetails(anyString());
    }

    @Test
    public void getMovieTasteOverlap_validToken_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        MovieTasteOverlapResultDTO responseDto = new MovieTasteOverlapResultDTO();
        responseDto.setMovieId("tt0111161");
        responseDto.setTasteOverlap(82);

        when(userService.authenticated("valid-token")).thenReturn(true);
        when(userService.getUserByToken("valid-token")).thenReturn(user);
        when(movieSearchService.getMovieTasteOverlap("tt0111161", user)).thenReturn(responseDto);

        mockMvc.perform(get("/movies/tt0111161/overlap")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId", is("tt0111161")))
                .andExpect(jsonPath("$.tasteOverlap", is(82)));

        verify(userService, times(1)).getUserByToken("valid-token");
        verify(movieSearchService, times(1)).getMovieTasteOverlap("tt0111161", user);
    }

    @Test
    public void getMovieTasteOverlap_invalidToken_returns401() throws Exception {
        when(userService.authenticated("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/movies/tt0111161/overlap")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getUserByToken(anyString());
        verify(movieSearchService, never()).getMovieTasteOverlap(anyString(), any(User.class));
    }
}