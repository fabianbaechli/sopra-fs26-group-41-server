package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class MovieSearchResponseDTO {
    private List<MovieSearchResultDTO> results = new ArrayList<>();

    public List<MovieSearchResultDTO> getResults() {
        return results;
    }

    public void setResults(List<MovieSearchResultDTO> results) {
        this.results = results;
    }
}