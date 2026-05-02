package ch.uzh.ifi.hase.soprafs26.rest.dto.drawing;

public class DrawingStateEventDTO {
    private String type = "drawing";
    private String event = "state";
    private String sessionId;
    private DrawingStateDTO drawingState;

    public DrawingStateEventDTO() {
    }

    public DrawingStateEventDTO(String sessionId, DrawingStateDTO drawingState) {
        this.sessionId = sessionId;
        this.drawingState = drawingState;
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

    public DrawingStateDTO getDrawingState() {
        return drawingState;
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

    public void setDrawingState(DrawingStateDTO drawingState) {
        this.drawingState = drawingState;
    }
}