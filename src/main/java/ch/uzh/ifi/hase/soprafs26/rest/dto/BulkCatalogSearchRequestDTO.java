package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class BulkCatalogSearchRequestDTO {
    public static class MovieSearchItem {
        private String name;
        private Integer year;

        public MovieSearchItem(String name, Integer year) {
            this.name = name;
            this.year = year;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
    }

    private List<MovieSearchItem> movies;

    public BulkCatalogSearchRequestDTO() {}

    public BulkCatalogSearchRequestDTO(List<MovieSearchItem> movies) {
        this.movies = movies;
    }

    public List<MovieSearchItem> getMovies() { return movies; }
    public void setMovies(List<MovieSearchItem> movies) { this.movies = movies; }
}
