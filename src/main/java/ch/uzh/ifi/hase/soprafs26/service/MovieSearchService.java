package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.RatedMovie;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CatalogMovieDTO;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
public class MovieSearchService {

    @Value("${omdb.api.key}")
    private String apiKey;

    @Value("${omdb.base.url}")
    private String baseUrl;

    @Value("${recommendation.service.url}") //Will need to adjust as soon as i have the implementation ready from my friend
    private String recommendationUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getGoogleCloudToken(String audienceUrl) {
        try {
            RestTemplate metadataTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Metadata-Flavor", "Google");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // This calls the internal Google server (only works when deployed to App Engine)
            ResponseEntity<String> response = metadataTemplate.exchange(
                    "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience=" + audienceUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return response.getBody();
        } catch (Exception e) {
            // If testing locally on your laptop, this will fail. Return null to fallback to unauthenticated.
            return null;
        }
    }

    public MovieSearchResponseDTO searchMovies(String query) {
        String sanitizedQuery = query != null ? query.replaceAll("[-–]", " ") : query;

        URI url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("s", sanitizedQuery)
                .queryParam("type", "movie")
                .queryParam("r", "json")
                .build()
                .encode()
                .toUri();

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
        return result;
    }

    public MovieTasteOverlapResultDTO getMovieTasteOverlap(String movieId, User user) {
        MovieDetailsResultDTO movieDetails = searchMovieDetails(movieId);

        MovieTasteOverlapResultDTO result = new MovieTasteOverlapResultDTO();
        result.setMovieId(movieDetails.getId());
        result.setTasteOverlap(null);

        String internalMovieId = fetchInternalMovieId(movieDetails.getTitle());
        if (internalMovieId == null) {
            return result;
        }

        Map<String, Float> watchedRatings = new HashMap<>();
        if (user != null && user.getTasteProfile() != null) {
            watchedRatings = user.getTasteProfile().getRatedMovies().stream()
                    .filter(m -> m.getMovieId() != null && !m.getMovieId().isEmpty())
                    .collect(Collectors.toMap(
                            RatedMovie::getMovieId,
                            RatedMovie::getRating,
                            (existing, replacement) -> existing
                    ));
        }

        try {
            OverlapRequestDTO requestPayload = new OverlapRequestDTO(watchedRatings, internalMovieId);
            String overlapUrl = recommendationUrl + "/calculate-overlap";

            HttpHeaders headers = new HttpHeaders();
            String token = getGoogleCloudToken(recommendationUrl);
            if (token != null) {
                headers.setBearerAuth(token);
            }
            HttpEntity<OverlapRequestDTO> requestEntity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<OverlapResponseDTO> responseEntity = restTemplate.exchange(
                    overlapUrl,
                    HttpMethod.POST,
                    requestEntity,
                    OverlapResponseDTO.class
            );

            OverlapResponseDTO overlapResponse = responseEntity.getBody();
            if (overlapResponse != null && overlapResponse.getOverlap_score() != null) {
                int percentageScore = (int) Math.round(overlapResponse.getOverlap_score() * 100);
                result.setTasteOverlap(percentageScore);
            } else {
                result.setTasteOverlap(0);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch taste overlap: " + e.getMessage());
        }

        return result;
    }
    public Integer getUserTasteOverlap(User currentUser, User targetUser) {
        if (currentUser == null || targetUser == null ||
                currentUser.getTasteProfile() == null || targetUser.getTasteProfile() == null ||
                currentUser.getTasteProfile().getRatedMovies() == null ||
                targetUser.getTasteProfile().getRatedMovies() == null) {
            return 0;
        }

        // Map Movie IDs to their actual ratings
        Map<String, Number> currentUserRatings = currentUser.getTasteProfile().getRatedMovies().stream()
                .collect(Collectors.toMap(
                        RatedMovie::getMovieId,
                        RatedMovie::getRating,
                        (existing, replacement) -> existing // Pragmatic safeguard against duplicate movie entries
                ));

        Map<String, Number> targetUserRatings = targetUser.getTasteProfile().getRatedMovies().stream()
                .collect(Collectors.toMap(
                        RatedMovie::getMovieId,
                        RatedMovie::getRating,
                        (existing, replacement) -> existing
                ));

        if (currentUserRatings.isEmpty() || targetUserRatings.isEmpty()) {
            return 0;
        }

        try {
            String url = recommendationUrl + "/user-overlap";
            HttpHeaders headers = new HttpHeaders();
            String token = getGoogleCloudToken(recommendationUrl);
            if (token != null) {
                headers.setBearerAuth(token);
            }

            // Create payload holding both maps
            Map<String, Map<String, Number>> requestPayload = new HashMap<>();
            requestPayload.put("user1_ratings", currentUserRatings);
            requestPayload.put("user2_ratings", targetUserRatings);

            HttpEntity<Map<String, Map<String, Number>>> requestEntity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, Map.class
            );

            Map<String, Object> responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.containsKey("overlap_score")) {
                return (Integer) responseBody.get("overlap_score");
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch weighted user taste overlap: " + e.getMessage());
        }

        return 0;
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

    public String fetchInternalMovieId(String movieName) {
        if (movieName == null || movieName.trim().isEmpty()) {
            return null;
        }

        String url = UriComponentsBuilder.fromUriString(recommendationUrl)
                .path("/movie/search")
                .queryParam("q", movieName)
                .toUriString();

        try {
            HttpHeaders headers = new HttpHeaders();
            String token = getGoogleCloudToken(recommendationUrl);
            if (token != null) {
                headers.setBearerAuth(token);
            }

            // Use HttpEntity with headers, but no body (since it's a GET request)
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // Use exchange instead of getForObject to pass the headers
            ResponseEntity<CatalogMovieDTO[]> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    CatalogMovieDTO[].class
            );

            CatalogMovieDTO[] results = responseEntity.getBody();

            if (results != null && results.length > 0) {
                // Grab the first result's ID and convert to String
                return String.valueOf(results[0].getId());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch internal ID for movie '" + movieName + "': " + e.getMessage());
        }

        return null;
    }

    public Map<String, String> fetchInternalMovieIds(List<RatedMovie> movies) {
        Map<String, String> movieIdsByName = new LinkedHashMap<>();
        if (movies == null || movies.isEmpty()) return movieIdsByName;

        // Correctly map RatedMovie objects to the DTO items
        List<BulkCatalogSearchRequestDTO.MovieSearchItem> searchItems = movies.stream()
                .filter(m -> m.getName() != null && !m.getName().isBlank())
                .map(m -> new BulkCatalogSearchRequestDTO.MovieSearchItem(m.getName().trim(), m.getYear()))
                .collect(Collectors.toList());

        if (searchItems.isEmpty()) return movieIdsByName;

        String url = recommendationUrl + "/movie/search/bulk";

        try {
            HttpHeaders headers = new HttpHeaders();
            String token = getGoogleCloudToken(recommendationUrl);
            if (token != null) headers.setBearerAuth(token);

            BulkCatalogSearchRequestDTO requestPayload = new BulkCatalogSearchRequestDTO(searchItems);
            HttpEntity<BulkCatalogSearchRequestDTO> requestEntity = new HttpEntity<>(requestPayload, headers);

            ResponseEntity<Map<String, CatalogMovieDTO[]>> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity,
                    new ParameterizedTypeReference<Map<String, CatalogMovieDTO[]>>() {}
            );

            Map<String, CatalogMovieDTO[]> results = responseEntity.getBody();
            if (results != null) {
                for (Map.Entry<String, CatalogMovieDTO[]> entry : results.entrySet()) {
                    CatalogMovieDTO[] matches = entry.getValue();
                    if (matches != null && matches.length > 0) {
                        movieIdsByName.put(entry.getKey(), String.valueOf(matches[0].getId()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch internal IDs in bulk: " + e.getMessage());
        }
        return movieIdsByName;
    }

    public List<RecommendResponseDTO> fetchRecommendations(Map<String, Float> watchedRatings, int limit, int offset) {
        if (watchedRatings == null || watchedRatings.isEmpty()) {
            return new ArrayList<>();
        }

        String url = recommendationUrl + "/recommend";
        RecommendRequestDTO requestPayload = new RecommendRequestDTO(watchedRatings, limit, offset);

        try {
            HttpHeaders headers = new HttpHeaders();
            String token = getGoogleCloudToken(recommendationUrl);
            if (token != null) {
                headers.setBearerAuth(token); // Use the same auth logic as your other methods
            }

            HttpEntity<RecommendRequestDTO> requestEntity = new HttpEntity<>(requestPayload, headers);

            // Use the class-level restTemplate and exchange() to pass the auth headers
            ResponseEntity<RecommendResponseDTO[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    RecommendResponseDTO[].class
            );

            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            // Log the error pragmatically and return empty so the app doesn't crash
            System.err.println("Failed to fetch recommendations: " + e.getMessage());
        }

        return new ArrayList<>();
    }
}