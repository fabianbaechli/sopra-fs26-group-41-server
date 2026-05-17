package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.ActiveDrawingUserDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStrokeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.PresenceUpdateEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.ActivePollMovieDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.ActivePollResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollDetailsGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollMovieDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollResultsResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollStartResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollStartedEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollVoteItemDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollVoteResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollVoteSubmissionDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void authenticationGetDTO_gettersAndSetters_roundTrip() {
        AuthenticationGetDTO dto = new AuthenticationGetDTO();

        dto.setToken("token");
        dto.setId(1L);
        dto.setUsername("alice");

        assertEquals("token", dto.getToken());
        assertEquals(1L, dto.getId());
        assertEquals("alice", dto.getUsername());
    }

    @Test
    void authenticationPostDTO_gettersAndSetters_roundTrip() {
        AuthenticationPostDTO dto = new AuthenticationPostDTO();

        dto.setUsername("alice");
        dto.setPassword("password");

        assertEquals("alice", dto.getUsername());
        assertEquals("password", dto.getPassword());
    }

    @Test
    void bulkCatalogSearchRequestDTO_constructorsGettersAndSetters_roundTrip() {
        BulkCatalogSearchRequestDTO.MovieSearchItem item =
                new BulkCatalogSearchRequestDTO.MovieSearchItem("The Matrix", 1999);

        assertEquals("The Matrix", item.getName());
        assertEquals(1999, item.getYear());

        item.setName("Inception");
        item.setYear(2010);

        assertEquals("Inception", item.getName());
        assertEquals(2010, item.getYear());

        BulkCatalogSearchRequestDTO emptyDto = new BulkCatalogSearchRequestDTO();
        emptyDto.setMovies(List.of(item));

        assertEquals(List.of(item), emptyDto.getMovies());

        BulkCatalogSearchRequestDTO constructorDto = new BulkCatalogSearchRequestDTO(List.of(item));

        assertEquals(List.of(item), constructorDto.getMovies());
    }

    @Test
    void bulkCatalogSearchResultDTO_gettersAndSetters_roundTrip() {
        CatalogMovieDTO movie = new CatalogMovieDTO();
        CatalogMovieDTO[] matches = new CatalogMovieDTO[]{movie};

        BulkCatalogSearchResultDTO dto = new BulkCatalogSearchResultDTO();

        dto.setQuery("matrix");
        dto.setMatches(matches);

        assertEquals("matrix", dto.getQuery());
        assertArrayEquals(matches, dto.getMatches());
    }

    @Test
    void catalogMovieDTO_gettersAndSetters_roundTrip() {
        CatalogMovieDTO dto = new CatalogMovieDTO();

        dto.setId(123);
        dto.setTitle("The Matrix");
        dto.setYear(1999);
        dto.setGenres("Action,Sci-Fi");

        assertEquals(123, dto.getId());
        assertEquals("The Matrix", dto.getTitle());
        assertEquals(1999, dto.getYear());
        assertEquals("Action,Sci-Fi", dto.getGenres());
    }

    @Test
    void groupCreatePostDTO_gettersAndSetters_roundTrip() {
        GroupCreatePostDTO dto = new GroupCreatePostDTO();

        dto.setName("Movie Club");

        assertEquals("Movie Club", dto.getName());
    }

    @Test
    void groupCreateResponseDTO_gettersAndSetters_roundTrip() {
        UserGetDTO owner = new UserGetDTO();
        owner.setId(1L);
        owner.setUsername("alice");

        GroupCreateResponseDTO dto = new GroupCreateResponseDTO();

        dto.setId(42L);
        dto.setName("Movie Club");
        dto.setOwner(owner);
        dto.setGroupProfilePicture("profile-picture");
        dto.setJoinUrl("join-url");

        assertEquals(42L, dto.getId());
        assertEquals("Movie Club", dto.getName());
        assertSame(owner, dto.getOwner());
        assertEquals("profile-picture", dto.getGroupProfilePicture());
        assertEquals("join-url", dto.getJoinUrl());
    }

    @Test
    void groupDetailsResponseDTO_gettersAndSetters_roundTrip() {
        GroupMemberDTO member = new GroupMemberDTO();
        member.setId(1L);
        member.setUsername("alice");

        GroupDetailsResponseDTO dto = new GroupDetailsResponseDTO();

        dto.setId(42L);
        dto.setName("Movie Club");
        dto.setOwnerId(1L);
        dto.setJoinUrl("join-url");
        dto.setGroupProfilePicture("profile-picture");
        dto.setMembers(List.of(member));

        assertEquals(42L, dto.getId());
        assertEquals("Movie Club", dto.getName());
        assertEquals(1L, dto.getOwnerId());
        assertEquals("join-url", dto.getJoinUrl());
        assertEquals("profile-picture", dto.getGroupProfilePicture());
        assertEquals(List.of(member), dto.getMembers());
    }

    @Test
    void groupGetResponseDTO_canBeInstantiated() {
        GroupGetResponseDTO dto = new GroupGetResponseDTO();

        assertNotNull(dto);
    }

    @Test
    void groupJoinResponseDTO_gettersAndSetters_roundTrip() {
        GroupJoinResponseDTO dto = new GroupJoinResponseDTO();

        dto.setGroupUrl("group-url");

        assertEquals("group-url", dto.getGroupUrl());
    }

    @Test
    void groupLeaveResponseDTO_gettersAndSetters_roundTrip() {
        GroupLeaveResponseDTO dto = new GroupLeaveResponseDTO();

        dto.setMessage("left group");

        assertEquals("left group", dto.getMessage());
    }

    @Test
    void groupMemberDTO_gettersAndSetters_roundTrip() {
        GroupMemberDTO dto = new GroupMemberDTO();

        dto.setId(1L);
        dto.setUsername("alice");

        assertEquals(1L, dto.getId());
        assertEquals("alice", dto.getUsername());
    }

    @Test
    void groupRecommendationDTO_gettersAndSetters_roundTrip() {
        RecommendedMovieDTO recommendation = new RecommendedMovieDTO();
        recommendation.setMovieId("tt0133093");

        GroupRecommendationDTO dto = new GroupRecommendationDTO();

        dto.setGroupId(42L);
        dto.setRecommendations(List.of(recommendation));

        assertEquals(42L, dto.getGroupId());
        assertEquals(List.of(recommendation), dto.getRecommendations());
    }

    @Test
    void groupSummaryDTO_gettersAndSetters_roundTrip() {
        GroupSummaryDTO dto = new GroupSummaryDTO();

        dto.setId(42L);
        dto.setName("Movie Club");
        dto.setGroupProfilePicture("profile-picture");
        dto.setMemberCount(3);

        assertEquals(42L, dto.getId());
        assertEquals("Movie Club", dto.getName());
        assertEquals("profile-picture", dto.getGroupProfilePicture());
        assertEquals(3, dto.getMemberCount());
    }

    @Test
    void groupsGetResponseDTO_gettersAndSetters_roundTrip() {
        GroupSummaryDTO summary = new GroupSummaryDTO();
        summary.setId(42L);

        GroupsGetResponseDTO dto = new GroupsGetResponseDTO();

        dto.setGroups(List.of(summary));

        assertEquals(List.of(summary), dto.getGroups());
    }

    @Test
    void movieDetailsResultDTO_gettersAndSetters_roundTrip() {
        MovieDetailsResultDTO dto = new MovieDetailsResultDTO();

        dto.setId("tt0133093");
        dto.setTitle("The Matrix");
        dto.setYear(1999);
        dto.setPosterUrl("poster-url");
        dto.setDescription("description");
        dto.setDirector("director");
        dto.setGenres("Action,Sci-Fi");
        dto.setRuntime("136 min");
        dto.setImdbRating("8.7");

        assertEquals("tt0133093", dto.getId());
        assertEquals("The Matrix", dto.getTitle());
        assertEquals(1999, dto.getYear());
        assertEquals("poster-url", dto.getPosterUrl());
        assertEquals("description", dto.getDescription());
        assertEquals("director", dto.getDirector());
        assertEquals("Action,Sci-Fi", dto.getGenres());
        assertEquals("136 min", dto.getRuntime());
        assertEquals("8.7", dto.getImdbRating());
    }

    @Test
    void movieSearchResponseDTO_defaultListAndSetters_roundTrip() {
        MovieSearchResponseDTO dto = new MovieSearchResponseDTO();

        assertNotNull(dto.getResults());
        assertTrue(dto.getResults().isEmpty());

        MovieSearchResultDTO result = new MovieSearchResultDTO();
        result.setId("tt0133093");

        dto.setResults(List.of(result));

        assertEquals(List.of(result), dto.getResults());
    }

    @Test
    void movieSearchResultDTO_gettersAndSetters_roundTrip() {
        MovieSearchResultDTO dto = new MovieSearchResultDTO();

        dto.setId("tt0133093");
        dto.setTitle("The Matrix");
        dto.setYear(1999);
        dto.setPosterUrl("poster-url");

        assertEquals("tt0133093", dto.getId());
        assertEquals("The Matrix", dto.getTitle());
        assertEquals(1999, dto.getYear());
        assertEquals("poster-url", dto.getPosterUrl());
    }

    @Test
    void movieTasteOverlapResultDTO_gettersAndSetters_roundTrip() {
        MovieTasteOverlapResultDTO dto = new MovieTasteOverlapResultDTO();

        dto.setMovieId("tt0133093");
        dto.setTasteOverlap(87);

        assertEquals("tt0133093", dto.getMovieId());
        assertEquals(87, dto.getTasteOverlap());
    }

    @Test
    void omdbSearchItemDTO_gettersAndSetters_roundTrip() {
        OmdbSearchItemDTO dto = new OmdbSearchItemDTO();

        dto.setImdbID("tt0133093");
        dto.setTitle("The Matrix");
        dto.setYear("1999");
        dto.setPoster("poster-url");

        assertEquals("tt0133093", dto.getImdbID());
        assertEquals("The Matrix", dto.getTitle());
        assertEquals("1999", dto.getYear());
        assertEquals("poster-url", dto.getPoster());
    }

    @Test
    void omdbSearchResponseDTO_gettersAndSetters_roundTrip() {
        OmdbSearchItemDTO item = new OmdbSearchItemDTO();
        item.setImdbID("tt0133093");

        OmdbSearchResponseDTO dto = new OmdbSearchResponseDTO();

        dto.setSearch(List.of(item));
        dto.setResponse("True");
        dto.setError("error");

        assertEquals(List.of(item), dto.getSearch());
        assertEquals("True", dto.getResponse());
        assertEquals("error", dto.getError());
    }

    @Test
    void overlapRequestDTO_constructorGettersAndSetters_roundTrip() {
        Map<String, Float> watchedRatings = Map.of("tt0133093", 4.5f);

        OverlapRequestDTO dto = new OverlapRequestDTO(watchedRatings, "tt0111161");

        assertEquals(watchedRatings, dto.getWatchedRatings());
        assertEquals("tt0111161", dto.getTarget_movie_id());

        Map<String, Float> updatedRatings = Map.of("tt0068646", 5.0f);
        dto.setWatchedRatings(updatedRatings);
        dto.setTarget_movie_id("tt0068646");

        assertEquals(updatedRatings, dto.getWatchedRatings());
        assertEquals("tt0068646", dto.getTarget_movie_id());
    }

    @Test
    void overlapResponseDTO_gettersAndSetters_roundTrip() {
        OverlapResponseDTO dto = new OverlapResponseDTO();

        dto.setTarget_id("tt0133093");
        dto.setOverlap_score(0.87);

        assertEquals("tt0133093", dto.getTarget_id());
        assertEquals(0.87, dto.getOverlap_score());
    }

    @Test
    void recommendRequestDTO_constructorGettersAndSetters_roundTrip() {
        Map<String, Float> watchedRatings = Map.of("tt0133093", 4.5f);

        RecommendRequestDTO dto = new RecommendRequestDTO(watchedRatings, 10, 5);

        assertEquals(watchedRatings, dto.getWatchedRatings());
        assertEquals(10, dto.getLimit());
        assertEquals(5, dto.getOffset());

        Map<String, Float> updatedRatings = Map.of("tt0068646", 5.0f);
        dto.setWatchedRatings(updatedRatings);
        dto.setLimit(20);
        dto.setOffset(10);

        assertEquals(updatedRatings, dto.getWatchedRatings());
        assertEquals(20, dto.getLimit());
        assertEquals(10, dto.getOffset());
    }

    @Test
    void recommendResponseDTO_gettersAndSetters_roundTrip() {
        RecommendResponseDTO dto = new RecommendResponseDTO();

        dto.setMovie_id("tt0133093");
        dto.setTitle("The Matrix");
        dto.setOverlap_score(0.91);

        assertEquals("tt0133093", dto.getMovie_id());
        assertEquals("The Matrix", dto.getTitle());
        assertEquals(0.91, dto.getOverlap_score());
    }

    @Test
    void recommendedMovieDTO_gettersAndSetters_roundTrip() {
        RecommendedMovieDTO dto = new RecommendedMovieDTO();

        dto.setMovieId("tt0133093");
        dto.setTitle("The Matrix");
        dto.setPosterUrl("poster-url");
        dto.setGroupMatchScore(92);

        assertEquals("tt0133093", dto.getMovieId());
        assertEquals("The Matrix", dto.getTitle());
        assertEquals("poster-url", dto.getPosterUrl());
        assertEquals(92, dto.getGroupMatchScore());
    }

    @Test
    void statsDTO_gettersAndSetters_roundTrip() {
        StatsDTO dto = new StatsDTO();

        dto.setMoviesLogged(10);
        dto.setHighlyRatedMovies(4);
        dto.setTopGenres(List.of("Drama", "Sci-Fi"));

        assertEquals(10, dto.getMoviesLogged());
        assertEquals(4, dto.getHighlyRatedMovies());
        assertEquals(List.of("Drama", "Sci-Fi"), dto.getTopGenres());
    }

    @Test
    void userGetDTO_gettersAndSetters_roundTrip() {
        UserGetDTO dto = new UserGetDTO();

        dto.setId(1L);
        dto.setUsername("alice");
        dto.setStatus(UserStatus.ONLINE);

        assertEquals(1L, dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals(UserStatus.ONLINE, dto.getStatus());
    }

    @Test
    void userPostDTO_gettersAndSetters_roundTrip() {
        UserPostDTO dto = new UserPostDTO();

        dto.setUsername("alice");
        dto.setPassword("password");

        assertEquals("alice", dto.getUsername());
        assertEquals("password", dto.getPassword());
    }

    @Test
    void usersGetDTO_gettersAndSetters_roundTrip() {
        StatsDTO stats = new StatsDTO();
        stats.setMoviesLogged(12);

        UsersGetDTO dto = new UsersGetDTO();

        dto.setId(1L);
        dto.setUsername("alice");
        dto.setHasLetterboxdData(true);
        dto.setTasteOverlap(88);
        dto.setStats(stats);

        assertEquals(1L, dto.getId());
        assertEquals("alice", dto.getUsername());
        assertTrue(dto.getHasLetterboxdData());
        assertEquals(88, dto.getTasteOverlap());
        assertSame(stats, dto.getStats());
    }

    @Test
    void activeDrawingUserDTO_constructorsGettersAndSetters_roundTrip() {
        ActiveDrawingUserDTO emptyDto = new ActiveDrawingUserDTO();

        emptyDto.setUserId(1L);
        emptyDto.setUsername("alice");

        assertEquals(1L, emptyDto.getUserId());
        assertEquals("alice", emptyDto.getUsername());

        ActiveDrawingUserDTO constructorDto = new ActiveDrawingUserDTO(2L, "bob");

        assertEquals(2L, constructorDto.getUserId());
        assertEquals("bob", constructorDto.getUsername());
    }

    @Test
    void drawingJoinResponseDTO_gettersAndSetters_roundTrip() {
        DrawingStateDTO state = new DrawingStateDTO();

        DrawingJoinResponseDTO dto = new DrawingJoinResponseDTO();

        dto.setSessionId("session-1");
        dto.setUserId(1L);
        dto.setDrawingState(state);

        assertEquals("session-1", dto.getSessionId());
        assertEquals(1L, dto.getUserId());
        assertSame(state, dto.getDrawingState());
    }

    @Test
    void drawingStateDTO_defaultListAndSetters_roundTrip() {
        DrawingStateDTO dto = new DrawingStateDTO();

        assertNotNull(dto.getStrokes());
        assertTrue(dto.getStrokes().isEmpty());

        DrawingStrokeDTO stroke = new DrawingStrokeDTO();
        stroke.setStrokeId("stroke-1");

        dto.setStrokes(List.of(stroke));

        assertEquals(List.of(stroke), dto.getStrokes());
    }

    @Test
    void drawingStateEventDTO_constructorsDefaultsGettersAndSetters_roundTrip() {
        DrawingStateDTO state = new DrawingStateDTO();

        DrawingStateEventDTO emptyDto = new DrawingStateEventDTO();

        assertEquals("drawing", emptyDto.getType());
        assertEquals("state", emptyDto.getEvent());

        emptyDto.setType("custom-type");
        emptyDto.setEvent("custom-event");
        emptyDto.setSessionId("session-1");
        emptyDto.setDrawingState(state);

        assertEquals("custom-type", emptyDto.getType());
        assertEquals("custom-event", emptyDto.getEvent());
        assertEquals("session-1", emptyDto.getSessionId());
        assertSame(state, emptyDto.getDrawingState());

        DrawingStateEventDTO constructorDto = new DrawingStateEventDTO("session-2", state);

        assertEquals("drawing", constructorDto.getType());
        assertEquals("state", constructorDto.getEvent());
        assertEquals("session-2", constructorDto.getSessionId());
        assertSame(state, constructorDto.getDrawingState());
    }

    @Test
    void drawingStrokeDTO_defaultListAndSetters_roundTrip() {
        DrawingStrokeDTO dto = new DrawingStrokeDTO();

        assertNotNull(dto.getPoints());
        assertTrue(dto.getPoints().isEmpty());

        List<List<Double>> points = List.of(
                List.of(10.0, 20.0),
                List.of(11.0, 21.0)
        );

        dto.setStrokeId("stroke-1");
        dto.setUserId(1L);
        dto.setTool("pen");
        dto.setColor("#000000");
        dto.setWidth(3.0);
        dto.setPoints(points);

        assertEquals("stroke-1", dto.getStrokeId());
        assertEquals(1L, dto.getUserId());
        assertEquals("pen", dto.getTool());
        assertEquals("#000000", dto.getColor());
        assertEquals(3.0, dto.getWidth());
        assertEquals(points, dto.getPoints());
    }

    @Test
    void presenceUpdateEventDTO_constructorsDefaultsGettersAndSetters_roundTrip() {
        ActiveDrawingUserDTO user = new ActiveDrawingUserDTO(1L, "alice");

        PresenceUpdateEventDTO emptyDto = new PresenceUpdateEventDTO();

        assertEquals("presence", emptyDto.getType());
        assertEquals("update", emptyDto.getEvent());

        emptyDto.setType("custom-type");
        emptyDto.setEvent("custom-event");
        emptyDto.setSessionId("session-1");
        emptyDto.setActiveUsers(List.of(user));

        assertEquals("custom-type", emptyDto.getType());
        assertEquals("custom-event", emptyDto.getEvent());
        assertEquals("session-1", emptyDto.getSessionId());
        assertEquals(List.of(user), emptyDto.getActiveUsers());

        PresenceUpdateEventDTO constructorDto = new PresenceUpdateEventDTO("session-2", List.of(user));

        assertEquals("presence", constructorDto.getType());
        assertEquals("update", constructorDto.getEvent());
        assertEquals("session-2", constructorDto.getSessionId());
        assertEquals(List.of(user), constructorDto.getActiveUsers());
    }

    @Test
    void activePollMovieDTO_gettersAndSetters_roundTrip() {
        ActivePollMovieDTO dto = new ActivePollMovieDTO();

        dto.setTitle("The Matrix");
        dto.setMovieId("tt0133093");
        dto.setDescription("description");
        dto.setDirector("director");
        dto.setGenres("Action,Sci-Fi");
        dto.setRuntime("136 min");
        dto.setImdbRating("8.7");
        dto.setPosterUrl("poster-url");
        dto.setTasteOverlap(91);

        assertEquals("The Matrix", dto.getTitle());
        assertEquals("tt0133093", dto.getMovieId());
        assertEquals("description", dto.getDescription());
        assertEquals("director", dto.getDirector());
        assertEquals("Action,Sci-Fi", dto.getGenres());
        assertEquals("136 min", dto.getRuntime());
        assertEquals("8.7", dto.getImdbRating());
        assertEquals("poster-url", dto.getPosterUrl());
        assertEquals(91, dto.getTasteOverlap());
    }

    @Test
    void activePollResponseDTO_gettersAndSetters_roundTrip() {
        ActivePollMovieDTO movie = new ActivePollMovieDTO();
        movie.setMovieId("tt0133093");

        ActivePollResponseDTO dto = new ActivePollResponseDTO();

        dto.setGroupId(42L);
        dto.setStatus("OPEN");
        dto.setPollCompletedByUser(true);
        dto.setMovies(List.of(movie));

        assertEquals(42L, dto.getGroupId());
        assertEquals("OPEN", dto.getStatus());
        assertTrue(dto.isPollCompletedByUser());
        assertEquals(List.of(movie), dto.getMovies());
    }

    @Test
    void pollDetailsGetDTO_gettersAndSetters_roundTrip() {
        PollMovieDTO movie = new PollMovieDTO();
        movie.setMovieId("tt0133093");

        PollDetailsGetDTO dto = new PollDetailsGetDTO();

        dto.setGroupId(42L);
        dto.setStatus("OPEN");
        dto.setPollCompletedByUser(true);
        dto.setMovies(List.of(movie));

        assertEquals(42L, dto.getGroupId());
        assertEquals("OPEN", dto.getStatus());
        assertTrue(dto.getPollCompletedByUser());
        assertEquals(List.of(movie), dto.getMovies());
    }

    @Test
    void pollMovieDTO_gettersAndSetters_roundTrip() {
        PollMovieDTO dto = new PollMovieDTO();

        dto.setMovieId("tt0133093");
        dto.setTitle("The Matrix");
        dto.setDescription("description");
        dto.setDirector("director");
        dto.setGenres(List.of("Action", "Sci-Fi"));
        dto.setRuntime(136);
        dto.setImdbRating(8.7f);
        dto.setPosterUrl("poster-url");
        dto.setVotes(5);
        dto.setTasteOverlap(91);

        assertEquals("tt0133093", dto.getMovieId());
        assertEquals("The Matrix", dto.getTitle());
        assertEquals("description", dto.getDescription());
        assertEquals("director", dto.getDirector());
        assertEquals(List.of("Action", "Sci-Fi"), dto.getGenres());
        assertEquals(136, dto.getRuntime());
        assertEquals(8.7f, dto.getImdbRating());
        assertEquals("poster-url", dto.getPosterUrl());
        assertEquals(5, dto.getVotes());
        assertEquals(91, dto.getTasteOverlap());
    }

    @Test
    void pollResultsResponseDTO_gettersAndSetters_roundTrip() {
        PollMovieDTO movie = new PollMovieDTO();
        movie.setMovieId("tt0133093");

        PollResultsResponseDTO dto = new PollResultsResponseDTO();

        dto.setStatus("FINISHED");
        dto.setTopMovies(List.of(movie));

        assertEquals("FINISHED", dto.getStatus());
        assertEquals(List.of(movie), dto.getTopMovies());
    }

    @Test
    void pollStartResponseDTO_gettersAndSetters_roundTrip() {
        PollMovieDTO movie = new PollMovieDTO();
        movie.setMovieId("tt0133093");

        PollStartResponseDTO dto = new PollStartResponseDTO();

        dto.setGroupId(42L);
        dto.setStatus("OPEN");
        dto.setMovies(List.of(movie));

        assertEquals(42L, dto.getGroupId());
        assertEquals("OPEN", dto.getStatus());
        assertEquals(List.of(movie), dto.getMovies());
    }

    @Test
    void pollStartedEventDTO_gettersAndSetters_roundTrip() {
        PollStartedEventDTO dto = new PollStartedEventDTO();

        dto.setType("poll");
        dto.setEvent("started");
        dto.setGroupId(42L);
        dto.setMessage("Poll started");
        dto.setUrl("/groups/42/poll");

        assertEquals("poll", dto.getType());
        assertEquals("started", dto.getEvent());
        assertEquals(42L, dto.getGroupId());
        assertEquals("Poll started", dto.getMessage());
        assertEquals("/groups/42/poll", dto.getUrl());
    }

    @Test
    void pollVoteItemDTO_gettersAndSetters_roundTrip() {
        PollVoteItemDTO dto = new PollVoteItemDTO();

        dto.setMovieId("tt0133093");
        dto.setInterested(true);

        assertEquals("tt0133093", dto.getMovieId());
        assertTrue(dto.isInterested());
    }

    @Test
    void pollVoteResponseDTO_gettersAndSetters_roundTrip() {
        PollVoteResponseDTO dto = new PollVoteResponseDTO();

        dto.setUserId(1L);

        assertEquals(1L, dto.getUserId());
    }

    @Test
    void pollVoteSubmissionDTO_gettersAndSetters_roundTrip() {
        PollVoteItemDTO vote = new PollVoteItemDTO();
        vote.setMovieId("tt0133093");

        PollVoteSubmissionDTO dto = new PollVoteSubmissionDTO();

        dto.setVotes(List.of(vote));

        assertEquals(List.of(vote), dto.getVotes());
    }
}