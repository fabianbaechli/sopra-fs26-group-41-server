package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

public class PollVoteItemDTO {
    private String movieId;
    private boolean interested;

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public boolean isInterested() {
        return interested;
    }

    public void setInterested(boolean interested) {
        this.interested = interested;
    }
}
