package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

import java.util.List;

public class ActivePollResponseDTO {
    private Long groupId;
    private String status;
    private boolean pollCompletedByUser;
    private List<ActivePollMovieDTO> movies;

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

    public boolean isPollCompletedByUser() {
        return pollCompletedByUser;
    }

    public void setPollCompletedByUser(boolean pollCompletedByUser) {
        this.pollCompletedByUser = pollCompletedByUser;
    }

    public List<ActivePollMovieDTO> getMovies() {
        return movies;
    }

    public void setMovies(List<ActivePollMovieDTO> movies) {
        this.movies = movies;
    }
}
