package ch.uzh.ifi.hase.soprafs26.rest.dto.drawing;

import java.util.ArrayList;
import java.util.List;

public class DrawingStateDTO {
    private List<DrawingStrokeDTO> strokes = new ArrayList<>();

    public List<DrawingStrokeDTO> getStrokes() {
        return strokes;
    }

    public void setStrokes(List<DrawingStrokeDTO> strokes) {
        this.strokes = strokes;
    }
}