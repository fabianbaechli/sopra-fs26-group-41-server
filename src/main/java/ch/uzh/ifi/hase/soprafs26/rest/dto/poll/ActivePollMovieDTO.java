package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

import java.util.List;

public class ActivePollMovieDTO { //I had to remove the inheritance from PollMovieDTO i hope this doesn't fuck anyone over. It was due to variables having different types and it not being overridable
    private String title;
    private String movieId;
    private String description;
    private String director;
    private String genres;
    private String runtime;
    private String imdbRating;
    private String posterUrl;
    private Integer tasteOverlap;


    public String getMovieId() {
        return movieId;
    }
    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getDescription() {return description;}

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

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public Integer getTasteOverlap() { return tasteOverlap; }
    public void setTasteOverlap(Integer tasteOverlap) { this.tasteOverlap = tasteOverlap; }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

}
