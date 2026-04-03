package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.Collection;

public class MovieDetailsResultDTO {
    private String id;
    private String title;
    private Integer year;
    private String posterUrl;
    private String description;
    private String director;
    private String genres;
    private String runtime;
    private String imdbRating;
    private Integer tasteOverlap;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(String imdbRating) {
        this.imdbRating = imdbRating;
    }

    public Integer getTasteOverlap() {
        return tasteOverlap;
    }

    public void setTasteOverlap(Integer tasteOverlap) {
        this.tasteOverlap = tasteOverlap;
    }
}
