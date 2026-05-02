package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStrokeDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.AppWebSocketHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DrawingService {

    private final GroupService groupService;
    private final AppWebSocketHandler appWebSocketHandler;
    private final ObjectMapper objectMapper;

    private final Map<Long, DrawingSession> drawingSessionsByGroupId = new ConcurrentHashMap<>();

    public DrawingService(
            GroupService groupService,
            AppWebSocketHandler appWebSocketHandler,
            ObjectMapper objectMapper
    ) {
        this.groupService = groupService;
        this.appWebSocketHandler = appWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    public DrawingJoinResponseDTO initializeDrawingSession(Long groupId, User user) {
        Group group = groupService.getGroupById(groupId);

        if (!groupService.isMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        DrawingSession session = drawingSessionsByGroupId.computeIfAbsent(
                groupId,
                ignored -> new DrawingSession(UUID.randomUUID().toString())
        );

        DrawingStateDTO drawingState = buildDrawingState(session);

        DrawingJoinResponseDTO response = new DrawingJoinResponseDTO();
        response.setSessionId(session.getSessionId());
        response.setUserId(user.getId());
        response.setDrawingState(drawingState);

        sendCurrentStateToUser(user, session.getSessionId(), drawingState);

        return response;
    }

    private DrawingStateDTO buildDrawingState(DrawingSession session) {
        DrawingStateDTO drawingState = new DrawingStateDTO();
        drawingState.setStrokes(new ArrayList<>(session.getStrokes()));
        return drawingState;
    }

    private void sendCurrentStateToUser(User user, String sessionId, DrawingStateDTO drawingState) {
        DrawingStateEventDTO event = new DrawingStateEventDTO(sessionId, drawingState);

        try {
            String payload = objectMapper.writeValueAsString(event);
            appWebSocketHandler.sendToUser(user.getUsername(), payload);
        } catch (Exception ignored) {
        }
    }

    private static class DrawingSession {
        private final String sessionId;
        private final List<DrawingStrokeDTO> strokes = new ArrayList<>();

        private DrawingSession(String sessionId) {
            this.sessionId = sessionId;
        }

        private String getSessionId() {
            return sessionId;
        }

        private List<DrawingStrokeDTO> getStrokes() {
            return strokes;
        }
    }
}