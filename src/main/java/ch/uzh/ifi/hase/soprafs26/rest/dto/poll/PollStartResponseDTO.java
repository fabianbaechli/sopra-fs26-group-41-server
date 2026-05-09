package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

import java.util.List;

public class PollStartResponseDTO {
    private Long groupId;
    private String status;
    private List<PollMovieDTO> movies;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PollMovieDTO> getMovies() {
        return movies;
    }

    public void setMovies(List<PollMovieDTO> movies) {
        this.movies = movies;
    }
}
