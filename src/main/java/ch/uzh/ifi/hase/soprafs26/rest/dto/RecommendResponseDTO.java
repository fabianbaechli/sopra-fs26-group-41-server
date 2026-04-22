package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class RecommendResponseDTO {
    private String movie_id;
    private String title;
    private double overlap_score;

    public String getMovie_id() { return movie_id; }
    public void setMovie_id(String movie_id) { this.movie_id = movie_id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getOverlap_score() { return overlap_score; }
    public void setOverlap_score(double overlap_score) { this.overlap_score = overlap_score; }
}