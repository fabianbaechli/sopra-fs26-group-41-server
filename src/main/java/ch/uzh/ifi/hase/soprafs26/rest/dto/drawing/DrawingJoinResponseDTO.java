package ch.uzh.ifi.hase.soprafs26.rest.dto.drawing;

public class DrawingJoinResponseDTO {
    private String sessionId;
    private Long userId;
    private DrawingStateDTO drawingState;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DrawingStateDTO getDrawingState() {
        return drawingState;
    }

    public void setDrawingState(DrawingStateDTO drawingState) {
        this.drawingState = drawingState;
    }
}