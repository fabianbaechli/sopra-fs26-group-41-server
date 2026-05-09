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
}
