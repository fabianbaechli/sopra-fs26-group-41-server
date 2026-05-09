package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollMovieDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.AppWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class PollBroadcastServiceTest {

    @Mock
    private AppWebSocketHandler webSocketHandler;

    @InjectMocks
    private PollBroadcastService pollBroadcastService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        pollBroadcastService = new PollBroadcastService(webSocketHandler, new ObjectMapper());
    }

    @Test
    public void broadcastPollStarted_success() {
        Group group = new Group();
        group.setId(1L);

        User user1 = new User();
        user1.setUsername("user1");

        User user2 = new User();
        user2.setUsername("user2");

        group.addMember(user1);
        group.addMember(user2);

        pollBroadcastService.broadcastPollStarted(group);

        Mockito.verify(webSocketHandler, Mockito.times(1)).sendToUser(eq("user1"), anyString());
        Mockito.verify(webSocketHandler, Mockito.times(1)).sendToUser(eq("user2"), anyString());
    }

    @Test
    public void broadcastPollFinished_success() {
        Group group = new Group();
        group.setId(1L);

        User user1 = new User();
        user1.setUsername("user1");
        group.addMember(user1);

        PollMovieDTO winner = new PollMovieDTO();
        winner.setMovieId("tt0111161");
        winner.setTitle("The Shawshank Redemption");

        pollBroadcastService.broadcastPollFinished(group, 99L, winner);

        Mockito.verify(webSocketHandler, Mockito.times(1)).sendToUser(eq("user1"), anyString());
    }

    @Test
    public void broadcastPollStarted_nullMembers_doesNothing() {
        Group group = new Group();
        group.setId(1L);

        pollBroadcastService.broadcastPollStarted(group);

        Mockito.verify(webSocketHandler, Mockito.never()).sendToUser(anyString(), anyString());
    }
}
