package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbSearchItemDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbSearchResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MovieSearchServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MovieSearchService movieSearchService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Inject fields dynamically
        ReflectionTestUtils.setField(movieSearchService, "apiKey", "test-key");
        ReflectionTestUtils.setField(movieSearchService, "baseUrl", "http://test-url.com/");
        ReflectionTestUtils.setField(movieSearchService, "recommendationUrl", "http://rec-url.com/");

        // Replace the internally instantiated RestTemplate with our mock
        ReflectionTestUtils.setField(movieSearchService, "restTemplate", restTemplate);
    }

    @Test
    public void searchMovies_success() {
        OmdbSearchResponseDTO mockResponse = new OmdbSearchResponseDTO();
        mockResponse.setResponse("True");

        OmdbSearchItemDTO item = new OmdbSearchItemDTO();
        item.setImdbID("tt123");
        item.setTitle("Inception");
        item.setYear("2010");
        mockResponse.setSearch(List.of(item));

        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(OmdbSearchResponseDTO.class)))
                .thenReturn(mockResponse);

        MovieSearchResponseDTO result = movieSearchService.searchMovies("Inception");

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals("tt123", result.getResults().get(0).getId());
        assertEquals("Inception", result.getResults().get(0).getTitle());
        assertEquals(2010, result.getResults().get(0).getYear());
    }

    @Test
    public void searchMovies_nullResponse_throwsException() {
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(OmdbSearchResponseDTO.class)))
                .thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> movieSearchService.searchMovies("Unknown"));
    }
    @Test
    public void searchMovieDetails_success() {
        ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbMovieDetails mockDetails = new ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbMovieDetails();
        mockDetails.setResponse("True");
        mockDetails.setImdbID("tt123");
        mockDetails.setTitle("Inception");
        mockDetails.setYear("2010");
        mockDetails.setDescription("A thief who steals corporate secrets...");

        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbMovieDetails.class)))
                .thenReturn(mockDetails);

        ch.uzh.ifi.hase.soprafs26.rest.dto.MovieDetailsResultDTO result = movieSearchService.searchMovieDetails("tt123");

        assertNotNull(result);
        assertEquals("tt123", result.getId());
        assertEquals("Inception", result.getTitle());
        assertEquals("A thief who steals corporate secrets...", result.getDescription());
    }

    @Test
    public void searchMovieDetails_notFound_throwsException() {
        ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbMovieDetails mockDetails = new ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbMovieDetails();
        mockDetails.setResponse("False");

        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(ch.uzh.ifi.hase.soprafs26.rest.dto.OmdbMovieDetails.class)))
                .thenReturn(mockDetails);

        assertThrows(ResponseStatusException.class, () -> movieSearchService.searchMovieDetails("tt999"));
    }

    @Test
    public void fetchInternalMovieId_nullOrEmpty_returnsNull() {
        assertNull(movieSearchService.fetchInternalMovieId(null));
        assertNull(movieSearchService.fetchInternalMovieId("   "));
    }

    @Test
    public void fetchRecommendations_nullOrEmptyList_returnsEmptyList() {
        assertTrue(movieSearchService.fetchRecommendations(null, 10, 0).isEmpty());
        assertTrue(movieSearchService.fetchRecommendations(java.util.Collections.emptyMap(), 10, 0).isEmpty());
    }

    @Test
    public void fetchInternalMovieIds_nullOrEmptyList_returnsEmptyMap() {
        assertTrue(movieSearchService.fetchInternalMovieIds(null).isEmpty());
        assertTrue(movieSearchService.fetchInternalMovieIds(List.of()).isEmpty());
    }
}