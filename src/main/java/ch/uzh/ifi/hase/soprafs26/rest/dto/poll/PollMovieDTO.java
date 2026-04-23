package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

import java.util.List;

public class PollMovieDTO {
    private String movieId; // Assuming ID from OMDB/Letterboxd is a String
    private String title;
    private String description;
    private String director;
    private List<String> genres;
    private Integer runtime;
    private Float imdbRating;
    private String posterUrl;
    private Integer tasteOverlap;

    // Standard Getters and Setters
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer runtime) { this.runtime = runtime; }

    public Float getImdbRating() { return imdbRating; }
    public void setImdbRating(Float imdbRating) { this.imdbRating = imdbRating; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public Integer getTasteOverlap() { return tasteOverlap; }
    public void setTasteOverlap(Integer tasteOverlap) { this.tasteOverlap = tasteOverlap; }
}