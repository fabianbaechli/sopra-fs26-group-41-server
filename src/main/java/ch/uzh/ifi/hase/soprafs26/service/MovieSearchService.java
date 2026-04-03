package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class MovieSearchService {

    @Value("${omdb.api.key}")
    private String apiKey;

    @Value("${omdb.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public MovieSearchResponseDTO searchMovies(String query) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("s", query)
                .queryParam("type", "movie")
                .queryParam("r", "json")
                .toUriString();

        OmdbSearchResponseDTO omdbResponse = restTemplate.getForObject(url, OmdbSearchResponseDTO.class);

        if (omdbResponse == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OMDb returned no response");
        }

        if ("False".equalsIgnoreCase(omdbResponse.getResponse())) {
            MovieSearchResponseDTO emptyResponse = new MovieSearchResponseDTO();
            emptyResponse.setResults(new ArrayList<>());
            return emptyResponse;
        }

        List<MovieSearchResultDTO> results = new ArrayList<>();
        if (omdbResponse.getSearch() != null) {
            for (OmdbSearchItemDTO item : omdbResponse.getSearch()) {
                MovieSearchResultDTO dto = new MovieSearchResultDTO();
                dto.setId(item.getImdbID());
                dto.setTitle(item.getTitle());
                dto.setYear(parseYear(item.getYear()));
                dto.setPosterUrl("N/A".equals(item.getPoster()) ? null : item.getPoster());
                results.add(dto);
            }
        }

        MovieSearchResponseDTO response = new MovieSearchResponseDTO();
        response.setResults(results);
        return response;
    }

    public MovieDetailsResultDTO searchMovieDetails(String movieId) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("i", movieId)
                .queryParam("type", "movie")
                .queryParam("r", "json")
                .queryParam("plot", "full")
                .toUriString();
        OmdbMovieDetails movieDetails = restTemplate.getForObject(url, OmdbMovieDetails.class);

        if (movieDetails == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OMDb returned no response");
        }

        if ("False".equalsIgnoreCase(movieDetails.getResponse())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found");
        }

        MovieDetailsResultDTO result = new MovieDetailsResultDTO();
        result.setId(movieDetails.getImdbID());
        result.setTitle(movieDetails.getTitle());
        result.setYear(parseYear(movieDetails.getYear()));
        result.setPosterUrl("N/A".equals(movieDetails.getPoster()) ? null : movieDetails.getPoster());
        result.setDescription(movieDetails.getDescription());
        result.setDirector(movieDetails.getDirector());
        result.setRuntime(movieDetails.getRuntime());
        result.setGenres(movieDetails.getGenre());
        result.setImdbRating(movieDetails.getImdbRating());
        // TODO: implement taste overlap
        result.setTasteOverlap(null);

        return result;
    }

    private Integer parseYear(String yearValue) {
        if (yearValue == null || yearValue.isBlank()) {
            return null;
        }

        String firstPart = yearValue.split("–|-")[0].trim();
        try {
            return Integer.parseInt(firstPart);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}