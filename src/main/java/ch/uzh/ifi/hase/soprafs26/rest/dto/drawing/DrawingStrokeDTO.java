package ch.uzh.ifi.hase.soprafs26.rest.dto.drawing;

import java.util.ArrayList;
import java.util.List;

public class DrawingStrokeDTO {
    private String strokeId;
    private Long userId;
    private String tool;
    private String color;
    private Double width;
    private List<List<Double>> points = new ArrayList<>();

    public String getStrokeId() {
        return strokeId;
    }

    public void setStrokeId(String strokeId) {
        this.strokeId = strokeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public List<List<Double>> getPoints() {
        return points;
    }

    public void setPoints(List<List<Double>> points) {
        this.points = points;
    }
}