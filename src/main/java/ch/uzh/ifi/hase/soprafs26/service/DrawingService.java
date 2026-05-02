package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Group;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.drawing.DrawingStateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DrawingService {

    private final GroupService groupService;

    /**
     * TODO: Add stroke tracking and websocket notifications
     */
    private final Map<Long, String> drawingSessionsByGroupId = new ConcurrentHashMap<>();

    public DrawingService(GroupService groupService) {
        this.groupService = groupService;
    }

    public DrawingJoinResponseDTO initializeDrawingSession(Long groupId, User user) {
        Group group = groupService.getGroupById(groupId);

        if (!groupService.isMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        // Only create session if not yet created
        String sessionId = drawingSessionsByGroupId.computeIfAbsent(
                groupId,
                ignored -> UUID.randomUUID().toString()
        );

        DrawingStateDTO drawingState = new DrawingStateDTO();

        DrawingJoinResponseDTO response = new DrawingJoinResponseDTO();
        response.setSessionId(sessionId);
        response.setUserId(user.getId());
        response.setDrawingState(drawingState);

        return response;
    }
}