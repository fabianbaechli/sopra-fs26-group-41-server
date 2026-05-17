package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.FetchedMovie;
import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollResultsResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollStartResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollVoteItemDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollVoteSubmissionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieDetailsResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.ActivePollResponseDTO;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PollServiceTest {

    @Mock
    private GroupService groupService;

    @Mock
    private PollBroadcastService pollBroadcastService;

    @Mock
    private MovieSearchService movieSearchService;

    @InjectMocks
    private PollService pollService;

    private Group testGroup;
    private User testUser;
    private User secondUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        secondUser = new User();
        secondUser.setId(2L);
        secondUser.setUsername("otherUser");

        FetchedMovie movie = new FetchedMovie();
        movie.setMovieId("tt0111161");
        movie.setName("The Shawshank Redemption");
        movie.setPosterUrl("poster-url");

        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.addMember(testUser);
        testGroup.addMember(secondUser);
        testGroup.setRecommendedMovies(List.of(movie));
    }

    @Test
    public void startPoll_validState_success() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        PollStartResponseDTO response = pollService.startPoll(1L, testUser);

        assertEquals(1L, response.getGroupId());
        assertEquals("OPEN", response.getStatus());
        assertEquals(1, response.getMovies().size());
        Mockito.verify(groupService, Mockito.never()).checkRecommendedMoviesEligibility(testGroup);
        Mockito.verify(groupService, Mockito.never()).recommendMovies(testGroup, 0);
        Mockito.verify(pollBroadcastService, Mockito.times(1)).broadcastPollStarted(testGroup);
    }

    @Test
    public void startPoll_notMember_throwsException() {
        User outsider = new User();
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, outsider)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> pollService.startPoll(1L, outsider));
    }

    @Test
    public void startPoll_concurrentPolls_throwsException() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        pollService.startPoll(1L, testUser);

        assertThrows(ResponseStatusException.class, () -> pollService.startPoll(1L, testUser));
    }

    @Test
    public void submitVotes_allMembersHaveVoted_finishesPoll() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);
        Mockito.when(groupService.isMember(testGroup, secondUser)).thenReturn(true);

        pollService.startPoll(1L, testUser);

        PollVoteSubmissionDTO firstSubmission = new PollVoteSubmissionDTO();
        PollVoteItemDTO firstVote = new PollVoteItemDTO();
        firstVote.setMovieId("tt0111161");
        firstVote.setInterested(true);
        firstSubmission.setVotes(List.of(firstVote));
        pollService.submitVotes(1L, testUser, firstSubmission);

        PollVoteSubmissionDTO secondSubmission = new PollVoteSubmissionDTO();
        PollVoteItemDTO secondVote = new PollVoteItemDTO();
        secondVote.setMovieId("tt0111161");
        secondVote.setInterested(true);
        secondSubmission.setVotes(List.of(secondVote));
        pollService.submitVotes(1L, secondUser, secondSubmission);

        PollResultsResponseDTO results = pollService.getPollResults(1L, testUser);
        assertEquals("FINISHED", results.getStatus());
        assertEquals(1, results.getTopMovies().size());
        assertEquals("tt0111161", results.getTopMovies().get(0).getMovieId());
        Mockito.verify(pollBroadcastService, Mockito.times(1))
                .broadcastPollFinished(Mockito.eq(testGroup), Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void getActivePoll_openPoll_returnsDetailedMoviesAndCompletionStatus() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        MovieDetailsResultDTO details = new MovieDetailsResultDTO();
        details.setDescription("Description");
        details.setDirector("Director");
        details.setGenres("Drama");
        details.setRuntime("142");
        details.setImdbRating("9.3");
        details.setPosterUrl("better-poster");

        Mockito.when(movieSearchService.searchMovieDetails("tt0111161")).thenReturn(details);

        pollService.startPoll(1L, testUser);

        ActivePollResponseDTO response = pollService.getActivePoll(1L, testUser);

        assertEquals(1L, response.getGroupId());
        assertEquals("OPEN", response.getStatus());
        assertFalse(response.isPollCompletedByUser());
        assertEquals(1, response.getMovies().size());
        assertEquals("tt0111161", response.getMovies().get(0).getMovieId());
        assertEquals("Description", response.getMovies().get(0).getDescription());
        assertEquals("Director", response.getMovies().get(0).getDirector());
        assertEquals("Drama", response.getMovies().get(0).getGenres());
        assertEquals("142", response.getMovies().get(0).getRuntime());
        assertEquals("9.3", response.getMovies().get(0).getImdbRating());
        assertEquals("better-poster", response.getMovies().get(0).getPosterUrl());
    }

    @Test
    public void getActivePoll_noOpenPoll_throwsNotFound() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> pollService.getActivePoll(1L, testUser)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    public void submitVotes_duplicateMovieIds_throwsBadRequest() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        pollService.startPoll(1L, testUser);

        PollVoteItemDTO vote1 = new PollVoteItemDTO();
        vote1.setMovieId("tt0111161");
        vote1.setInterested(true);

        PollVoteItemDTO vote2 = new PollVoteItemDTO();
        vote2.setMovieId("tt0111161");
        vote2.setInterested(false);

        PollVoteSubmissionDTO submission = new PollVoteSubmissionDTO();
        submission.setVotes(List.of(vote1, vote2));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> pollService.submitVotes(1L, testUser, submission)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void submitVotes_invalidMovieId_throwsBadRequest() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        pollService.startPoll(1L, testUser);

        PollVoteItemDTO vote = new PollVoteItemDTO();
        vote.setMovieId("invalid-id");
        vote.setInterested(true);

        PollVoteSubmissionDTO submission = new PollVoteSubmissionDTO();
        submission.setVotes(List.of(vote));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> pollService.submitVotes(1L, testUser, submission)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void submitVotes_sameUserVotesTwice_throwsConflict() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        pollService.startPoll(1L, testUser);

        PollVoteItemDTO vote = new PollVoteItemDTO();
        vote.setMovieId("tt0111161");
        vote.setInterested(true);

        PollVoteSubmissionDTO submission = new PollVoteSubmissionDTO();
        submission.setVotes(List.of(vote));

        pollService.submitVotes(1L, testUser, submission);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> pollService.submitVotes(1L, testUser, submission)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void getPollResults_beforePollFinished_throwsConflict() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        pollService.startPoll(1L, testUser);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> pollService.getPollResults(1L, testUser)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    public void startPoll_withoutCachedRecommendations_fetchesRecommendations() {
        testGroup.setRecommendedMovies(new java.util.ArrayList<>());

        FetchedMovie fetchedMovie = new FetchedMovie();
        fetchedMovie.setMovieId("tt0068646");
        fetchedMovie.setName("The Godfather");
        fetchedMovie.setPosterUrl("poster");

        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);
        Mockito.when(groupService.recommendMovies(testGroup, 0)).thenAnswer(invocation -> {
            testGroup.setRecommendedMovies(List.of(fetchedMovie));
            return List.of(fetchedMovie);
        });

        PollStartResponseDTO response = pollService.startPoll(1L, testUser);

        assertEquals("OPEN", response.getStatus());
        assertEquals(1, response.getMovies().size());
        assertEquals("tt0068646", response.getMovies().get(0).getMovieId());

        Mockito.verify(groupService).checkRecommendedMoviesEligibility(testGroup);
        Mockito.verify(groupService).recommendMovies(testGroup, 0);
    }

    @Test
    public void getPollDetails_notMember_throwsForbidden() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> pollService.getPollDetails(1L, testUser)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void getPollDetails_noActivePoll_throwsNotFound() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> pollService.getPollDetails(1L, testUser)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
