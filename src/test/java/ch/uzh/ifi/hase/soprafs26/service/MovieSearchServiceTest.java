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
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.TasteProfile;
import ch.uzh.ifi.hase.soprafs26.entity.RatedMovie;
import ch.uzh.ifi.hase.soprafs26.rest.dto.OverlapResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RecommendResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GroupOverlapResponseDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    @Test
    public void searchMovies_falseResponse_returnsEmptyList() {
        OmdbSearchResponseDTO mockResponse = new OmdbSearchResponseDTO();
        mockResponse.setResponse("False");

        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(OmdbSearchResponseDTO.class)))
                .thenReturn(mockResponse);

        MovieSearchResponseDTO result = movieSearchService.searchMovies("UnknownMovie123");

        assertNotNull(result);
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    public void searchMovies_parsesYearAndHandlesNA() {
        OmdbSearchResponseDTO mockResponse = new OmdbSearchResponseDTO();
        mockResponse.setResponse("True");

        OmdbSearchItemDTO item = new OmdbSearchItemDTO();
        item.setImdbID("tt456");
        item.setTitle("Test Movie");
        item.setYear("2020–2021"); // Testing the split logic
        item.setPoster("N/A"); // Testing the N/A logic
        mockResponse.setSearch(List.of(item));

        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(OmdbSearchResponseDTO.class)))
                .thenReturn(mockResponse);

        MovieSearchResponseDTO result = movieSearchService.searchMovies("Test Movie");

        assertEquals(2020, result.getResults().get(0).getYear());
        assertNull(result.getResults().get(0).getPosterUrl());
    }

    @Test
    public void getUserTasteOverlap_nullUsersOrProfiles_returnsZero() {
        User user1 = new User();
        User user2 = new User();
        // Taste profiles are null
        assertEquals(0, movieSearchService.getUserTasteOverlap(user1, user2));
        assertEquals(0, movieSearchService.getUserTasteOverlap(null, user2));
    }

    @Test
    public void getUserTasteOverlap_success() {
        User user1 = new User();
        TasteProfile tp1 = new TasteProfile();
        RatedMovie rm1 = new RatedMovie();
        rm1.setMovieId("101");
        rm1.setRating(8.0f);
        tp1.setRatedMovies(List.of(rm1));
        user1.setTasteProfile(tp1);

        User user2 = new User();
        TasteProfile tp2 = new TasteProfile();
        RatedMovie rm2 = new RatedMovie();
        rm2.setMovieId("101");
        rm2.setRating(7.0f);
        tp2.setRatedMovies(List.of(rm2));
        user2.setTasteProfile(tp2);

        Map<String, Object> mockBody = new HashMap<>();
        mockBody.put("overlap_score", 85);
        ResponseEntity<Map> mockResponseEntity = new ResponseEntity<>(mockBody, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(Map.class)
        )).thenReturn(mockResponseEntity);

        Integer overlap = movieSearchService.getUserTasteOverlap(user1, user2);
        assertEquals(85, overlap);
    }

    @Test
    public void fetchRecommendations_success() {
        Map<String, Float> ratings = new HashMap<>();
        ratings.put("101", 9.0f);

        RecommendResponseDTO rec1 = new RecommendResponseDTO();
        // Set basic fields if your DTO has them, e.g., rec1.setMovieId("102");
        RecommendResponseDTO[] mockResponseArray = { rec1 };
        ResponseEntity<RecommendResponseDTO[]> mockResponseEntity = new ResponseEntity<>(mockResponseArray, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(RecommendResponseDTO[].class)
        )).thenReturn(mockResponseEntity);

        List<RecommendResponseDTO> results = movieSearchService.fetchRecommendations(ratings, 10, 0);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    public void getGroupOverlap_success() {
        List<Map<String, Float>> memberRatings = new ArrayList<>();
        Map<String, Float> ratings = new HashMap<>();
        ratings.put("101", 8.0f);
        memberRatings.add(ratings);

        GroupOverlapResponseDTO mockDto = new GroupOverlapResponseDTO();
        // Assuming your DTO has a setter for overlap or similar

        ResponseEntity<GroupOverlapResponseDTO> mockResponseEntity = new ResponseEntity<>(mockDto, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(GroupOverlapResponseDTO.class)
        )).thenReturn(mockResponseEntity);

        GroupOverlapResponseDTO result = movieSearchService.GetGroupOverlap(memberRatings);
        assertNotNull(result);
    }

    @Test
    public void getGroupOverlap_exceptionCaught_returnsEmptyObject() {
        List<Map<String, Float>> memberRatings = new ArrayList<>();

        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.eq(GroupOverlapResponseDTO.class)
        )).thenThrow(new RuntimeException("Simulated Network Error"));

        GroupOverlapResponseDTO result = movieSearchService.GetGroupOverlap(memberRatings);

        assertNotNull(result); // Should return a new empty DTO instead of crashing
    }
}