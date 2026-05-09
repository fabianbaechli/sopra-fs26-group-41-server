package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class RatedMovie implements Serializable {

    @Column(name = "movie_id", nullable = false)
    private String movieId;

    @Column(name = "movie_name", nullable = false)
    private String name;

    @Column(name = "movie_rating", nullable = false)
    private float rating;
    @Column(name = "movie_year")
    private Integer year;

    public String getMovieId() {return movieId;}

    public void setMovieId(String movieId) {this.movieId = movieId;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public Integer getYear() {return year;}

    public void setYear(Integer year) {this.year = year;}

}
