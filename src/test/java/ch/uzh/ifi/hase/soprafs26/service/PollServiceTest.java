package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PollServiceTest {

    @Mock
    private GroupService groupService;

    @Mock
    private PollBroadcastService pollBroadcastService;

    @InjectMocks
    private PollService pollService;

    private Group testGroup;
    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setUsername("testUser");

        testGroup = new Group();
        testGroup.setId(1L);
        testGroup.addMember(testUser);
    }

    @Test
    public void startPoll_validState_success() {
        Mockito.when(groupService.getGroupById(1L)).thenReturn(testGroup);
        Mockito.when(groupService.isMember(testGroup, testUser)).thenReturn(true);

        pollService.startPoll(1L, testUser);

        Mockito.verify(groupService, Mockito.times(1)).checkRecommendedMoviesEligibility(testGroup);
        Mockito.verify(groupService, Mockito.times(1)).recommendMovies(testGroup, 0);
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

        // Start first poll successfully
        pollService.startPoll(1L, testUser);

        // Attempting to start another poll for the same group should throw conflict
        assertThrows(ResponseStatusException.class, () -> pollService.startPoll(1L, testUser));
    }
}