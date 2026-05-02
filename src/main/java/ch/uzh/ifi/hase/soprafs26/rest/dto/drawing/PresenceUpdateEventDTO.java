package ch.uzh.ifi.hase.soprafs26.rest.dto.drawing;

import java.util.List;

public class PresenceUpdateEventDTO {
    private String type = "presence";
    private String event = "update";
    private String sessionId;
    private List<ActiveDrawingUserDTO> activeUsers;

    public PresenceUpdateEventDTO() {
    }

    public PresenceUpdateEventDTO(String sessionId, List<ActiveDrawingUserDTO> activeUsers) {
        this.sessionId = sessionId;
        this.activeUsers = activeUsers;
    }

    public String getType() {
        return type;
    }

    public String getEvent() {
        return event;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<ActiveDrawingUserDTO> getActiveUsers() {
        return activeUsers;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setActiveUsers(List<ActiveDrawingUserDTO> activeUsers) {
        this.activeUsers = activeUsers;
    }
}