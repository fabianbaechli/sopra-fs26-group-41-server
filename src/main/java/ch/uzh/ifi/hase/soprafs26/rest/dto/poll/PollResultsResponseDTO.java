package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

import java.util.List;

public class PollResultsResponseDTO {
    private String status;
    private List<PollMovieDTO> topMovies;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PollMovieDTO> getTopMovies() {
        return topMovies;
    }

    public void setTopMovies(List<PollMovieDTO> topMovies) {
        this.topMovies = topMovies;
    }
}
