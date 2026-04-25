package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.Map;

public class RecommendRequestDTO {
    private Map<String,Float> watchedRatings;
    private int limit;
    private int offset;

    public RecommendRequestDTO(Map<String,Float> watchedRatings, int limit, int offset) {
        this.watchedRatings = watchedRatings;
        this.limit = limit;
        this.offset = offset;
    }

    public Map<String,Float> getWatchedRatings() { return watchedRatings; }
    public void setWatchedRatings(Map<String,Float> watchedRatings) { this.watchedRatings = watchedRatings; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}