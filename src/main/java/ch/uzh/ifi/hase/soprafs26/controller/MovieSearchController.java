package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieDetailsResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs26.service.MovieSearchService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import jakarta.websocket.server.PathParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MovieSearchController {
    private final UserService userService;
    private final MovieSearchService movieSearchService;
    MovieSearchController(UserService userService, MovieSearchService movieSearchService) {
        this.userService = userService;
        this.movieSearchService = movieSearchService;
    }

    @GetMapping("/movies/search")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public MovieSearchResponseDTO getMovieResults(
            @RequestHeader(value = "Authorization", required = true)  String authorizationHeader,
            @RequestParam("query") String searchQuery) {
        String token = authorizationHeader.replace("Bearer ", "");
        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }
        return movieSearchService.searchMovies(searchQuery);

    }

    @GetMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public MovieDetailsResultDTO getMovieDetails(
            @RequestHeader(value = "Authorization", required = true)  String authorizationHeader,
            @PathVariable String movieId) {
        String token = authorizationHeader.replace("Bearer ", "");
        if (!userService.authenticated(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You need to be logged in to do this");
        }

        return movieSearchService.searchMovieDetails(movieId);
    }
}
