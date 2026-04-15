package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class RecommendedMovieDTO {
    private String movieId;
    private String title;
    private String posterUrl;         // Placeholder for when you implement OMDb posters
    private Integer groupMatchScore;

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public Integer getGroupMatchScore() { return groupMatchScore; }
    public void setGroupMatchScore(Integer groupMatchScore) { this.groupMatchScore = groupMatchScore; }
}