package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.ActiveDrawingUserDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateEventDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStrokeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.PresenceUpdateEventDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.AppWebSocketHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


@Service
public class DrawingService {

    private final GroupService groupService;
    private final AppWebSocketHandler appWebSocketHandler;
    private final ObjectMapper objectMapper;

    private final Map<Long, DrawingSession> drawingSessionsByGroupId = new ConcurrentHashMap<>();
    private final Map<String, DrawingSession> sessionsById = new ConcurrentHashMap<>();


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

        sessionsById.put(session.getSessionId(), session);


        session.getActiveUsers().put(
                user.getId(),
                new ActiveDrawingUserDTO(user.getId(), user.getUsername())
        );

        DrawingStateDTO drawingState = buildDrawingState(session);

        DrawingJoinResponseDTO response = new DrawingJoinResponseDTO();
        response.setSessionId(session.getSessionId());
        response.setUserId(user.getId());
        response.setDrawingState(drawingState);

        sendCurrentStateToUser(user, session.getSessionId(), drawingState);
        broadcastPresenceUpdate(session);

        return response;
    }
    public void processWebSocketMessage(String username, String payload) {
        try {
            StrokeMessageDTO message = objectMapper.readValue(payload, StrokeMessageDTO.class);
            if ("stroke".equals(message.getType())) {
                if ("start".equals(message.getEvent())) {
                    handleStrokeStart(username, message);
                } else if ("append".equals(message.getEvent())) {
                    handleStrokeAppend(username, message);
                } else if ("end".equals(message.getEvent())) {
                    handleStrokeEnd(username, message);
                }
            }
        } catch (Exception ignored) {
            // Drop invalid/malformed JSON payloads silently
        }
    }

    private void handleStrokeStart(String username, StrokeMessageDTO message) throws Exception {
        DrawingSession session = sessionsById.get(message.getSessionId());
        if (session == null) return;

        Long userId = getUserIdByUsername(username, session);
        if (userId == null) return;

        DrawingStrokeDTO stroke = new DrawingStrokeDTO();
        stroke.setStrokeId(message.getStrokeId());
        stroke.setUserId(userId);
        stroke.setColor(message.getColor());
        stroke.setWidth(message.getWidth());

        if (message.getPoint() != null) {
            stroke.getPoints().add(message.getPoint());
        }

        session.getStrokes().add(stroke);

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "stroke");
        broadcast.put("event", "started");
        broadcast.put("sessionId", message.getSessionId());

        Map<String, Object> strokeData = new HashMap<>();
        strokeData.put("strokeId", message.getStrokeId());
        strokeData.put("userId", userId);
        strokeData.put("color", message.getColor());
        strokeData.put("width", message.getWidth());
        strokeData.put("point", message.getPoint());

        broadcast.put("stroke", strokeData);

        broadcastToSessionExcept(session, username, objectMapper.writeValueAsString(broadcast));
    }

    private void handleStrokeAppend(String username, StrokeMessageDTO message) throws Exception {
        DrawingSession session = sessionsById.get(message.getSessionId());
        if (session == null) return;

        session.getStrokes().stream()
                .filter(s -> s.getStrokeId().equals(message.getStrokeId()))
                .findFirst()
                .ifPresent(stroke -> {
                    if (message.getPoints() != null) {
                        stroke.getPoints().addAll(message.getPoints());
                    }
                });

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "stroke");
        broadcast.put("event", "appended");
        broadcast.put("sessionId", message.getSessionId());
        broadcast.put("strokeId", message.getStrokeId());
        broadcast.put("points", message.getPoints());

        broadcastToSessionExcept(session, username, objectMapper.writeValueAsString(broadcast));
    }

    private void handleStrokeEnd(String username, StrokeMessageDTO message) throws Exception {
        DrawingSession session = sessionsById.get(message.getSessionId());
        if (session == null) return;

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "stroke");
        broadcast.put("event", "ended");
        broadcast.put("sessionId", message.getSessionId());
        broadcast.put("strokeId", message.getStrokeId());

        broadcastToSessionExcept(session, username, objectMapper.writeValueAsString(broadcast));
    }

    private void broadcastToSessionExcept(DrawingSession session, String excludeUsername, String payload) {
        for (ActiveDrawingUserDTO activeUser : session.getActiveUsers().values()) {
            if (!activeUser.getUsername().equals(excludeUsername)) {
                appWebSocketHandler.sendToUser(activeUser.getUsername(), payload);
            }
        }
    }

    private Long getUserIdByUsername(String username, DrawingSession session) {
        for (ActiveDrawingUserDTO activeUser : session.getActiveUsers().values()) {
            if (activeUser.getUsername().equals(username)) {
                return activeUser.getUserId();
            }
        }
        return null;
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

    // lets users which already are in the drawing session know that a new user has joined
    private void broadcastPresenceUpdate(DrawingSession session) {
        List<ActiveDrawingUserDTO> activeUsers = session.getActiveUsers()
                .values()
                .stream()
                .sorted(Comparator.comparing(ActiveDrawingUserDTO::getUsername))
                .toList();

        PresenceUpdateEventDTO event = new PresenceUpdateEventDTO(
                session.getSessionId(),
                activeUsers
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            for (ActiveDrawingUserDTO activeUser : activeUsers) {
                appWebSocketHandler.sendToUser(activeUser.getUsername(), payload);
            }
        } catch (Exception ignored) {
        }
    }
    public static class StrokeMessageDTO {
        private String type;
        private String event;
        private String sessionId;
        private String strokeId;
        private String color;
        private Double width;
        private List<Double> point;
        private List<List<Double>> points;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getStrokeId() { return strokeId; }
        public void setStrokeId(String strokeId) { this.strokeId = strokeId; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public Double getWidth() { return width; }
        public void setWidth(Double width) { this.width = width; }
        public List<Double> getPoint() { return point; }
        public void setPoint(List<Double> point) { this.point = point; }
        public List<List<Double>> getPoints() { return points; }
        public void setPoints(List<List<Double>> points) { this.points = points; }
    }



    private static class DrawingSession {
        private final String sessionId;
        private final List<DrawingStrokeDTO> strokes = new ArrayList<>();
        private final Map<Long, ActiveDrawingUserDTO> activeUsers = new ConcurrentHashMap<>();

        private DrawingSession(String sessionId) {
            this.sessionId = sessionId;
        }

        private String getSessionId() {
            return sessionId;
        }

        private List<DrawingStrokeDTO> getStrokes() {
            return strokes;
        }

        private Map<Long, ActiveDrawingUserDTO> getActiveUsers() {
            return activeUsers;
        }
    }
}