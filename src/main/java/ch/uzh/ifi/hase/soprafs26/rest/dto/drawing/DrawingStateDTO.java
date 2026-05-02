package ch.uzh.ifi.hase.soprafs26.rest.dto.drawing;

import java.util.ArrayList;
import java.util.List;

public class DrawingStateDTO {
    // TODO: track actual strokes here. This is just <Object> is just temporary
    private List<Object> strokes = new ArrayList<>();

    public List<Object> getStrokes() {
        return strokes;
    }

    public void setStrokes(List<Object> strokes) {
        this.strokes = strokes;
    }
}