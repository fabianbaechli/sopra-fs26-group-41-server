package ch.uzh.ifi.hase.soprafs26.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OmdbSearchResponseDTO {
    @JsonProperty("Search")
    private List<OmdbSearchItemDTO> search;

    @JsonProperty("Response")
    private String response;

    @JsonProperty("Error")
    private String error;

    public List<OmdbSearchItemDTO> getSearch() {
        return search;
    }

    public void setSearch(List<OmdbSearchItemDTO> search) {
        this.search = search;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}