package ch.uzh.ifi.hase.soprafs26.rest.dto;
import java.util.List;
import java.util.Map;

public class OverlapRequestDTO {
    private Map<String,Float> watchedRatings;
    private String target_movie_id;


    public OverlapRequestDTO(Map<String,Float> watchedRatings, String target_movie_id) {
        this.watchedRatings = watchedRatings;
        this.target_movie_id = target_movie_id;
    }

    public Map<String,Float> getWatchedRatings() { return watchedRatings; }
    public void setWatchedRatings(Map<String,Float> watchedRatings) { this.watchedRatings = watchedRatings; }

    public String getTarget_movie_id() { return target_movie_id; }
    public void setTarget_movie_id(String target_movie_id) { this.target_movie_id = target_movie_id; }
}