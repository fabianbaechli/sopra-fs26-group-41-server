package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.poll.PollMovieDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.AppWebSocketHandler;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PollBroadcastService {

    private final AppWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public PollBroadcastService(AppWebSocketHandler webSocketHandler, ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    public void broadcastPollStarted(Group group) {
        if (group.getMembers() == null) {
            return;
        }

        String payload = toJson(buildPollStartedPayload(group.getId()));

        for (User member : group.getMembers()) {
            webSocketHandler.sendToUser(member.getUsername(), payload);
        }
    }

    public void broadcastPollFinished(Group group, Long pollId, PollMovieDTO topMovie) {
        if (group.getMembers() == null) {
            return;
        }

        String payload = toJson(buildPollFinishedPayload(group.getId(), pollId, topMovie));

        for (User member : group.getMembers()) {
            webSocketHandler.sendToUser(member.getUsername(), payload);
        }
    }

    private Map<String, Object> buildPollStartedPayload(Long groupId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "poll");
        payload.put("event", "started");
        payload.put("groupId", groupId);
        payload.put("message", "A new poll has started for this group.");
        payload.put("url", "/groups/" + groupId + "/poll");
        return payload;
    }

    private Map<String, Object> buildPollFinishedPayload(Long groupId, Long pollId, PollMovieDTO topMovie) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "poll");
        payload.put("event", "finished");
        payload.put("pollId", pollId);
        payload.put("groupId", groupId);
        payload.put("topMovie", topMovie);
        payload.put("message", "The poll has finished.");
        payload.put("url", "/groups/" + groupId + "/results");
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize poll websocket payload", e);
        }
    }
}
