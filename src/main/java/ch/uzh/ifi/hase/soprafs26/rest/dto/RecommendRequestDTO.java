package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class RecommendRequestDTO {
    private List<String> watched_ids;
    private int limit;
    private int offset;

    public RecommendRequestDTO(List<String> watched_ids, int limit, int offset) {
        this.watched_ids = watched_ids;
        this.limit = limit;
        this.offset = offset;
    }

    public List<String> getWatched_ids() { return watched_ids; }
    public void setWatched_ids(List<String> watched_ids) { this.watched_ids = watched_ids; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}