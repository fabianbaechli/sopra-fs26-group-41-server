package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class FetchedMovie implements Serializable {

    @Column(name = "movie_id", nullable = true)
    private String movieId;

    @Column(name = "movie_name", nullable = false)
    private String name;

    private String internalId;

    @Column(name = "overlap_score")
    private Double overlapScore;

    @Column(name = "poster_url", nullable = true)
    private String posterUrl;

    public String getPosterUrl() {return posterUrl;}
    public void setPosterUrl(String posterUrl) {this.posterUrl = posterUrl;}

    public String getMovieId() {return movieId;}

    public void setMovieId(String movieId) {this.movieId = movieId;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInternalId() {return internalId;};

    public void setInternalId(String internalId) {this.internalId = internalId;}

    public Double getOverlapScore() {return overlapScore;}

    public void setOverlapScore(Double overlapScore) {this.overlapScore = overlapScore;}
}
