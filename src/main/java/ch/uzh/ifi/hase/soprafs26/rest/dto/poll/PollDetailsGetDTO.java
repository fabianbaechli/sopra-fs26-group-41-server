package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

import java.util.List;

public class PollDetailsGetDTO {
    private Long groupId;
    private String status;
    private Boolean pollCompletedByUser;
    private List<PollMovieDTO> movies;

    // Standard Getters and Setters
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getPollCompletedByUser() { return pollCompletedByUser; }
    public void setPollCompletedByUser(Boolean pollCompletedByUser) { this.pollCompletedByUser = pollCompletedByUser; }

    public List<PollMovieDTO> getMovies() { return movies; }
    public void setMovies(List<PollMovieDTO> movies) { this.movies = movies; }
}