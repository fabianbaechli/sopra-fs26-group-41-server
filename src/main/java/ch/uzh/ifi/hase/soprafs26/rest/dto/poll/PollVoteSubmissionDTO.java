package ch.uzh.ifi.hase.soprafs26.rest.dto.poll;

import java.util.List;

public class PollVoteSubmissionDTO {
    private List<PollVoteItemDTO> votes;

    public List<PollVoteItemDTO> getVotes() {
        return votes;
    }

    public void setVotes(List<PollVoteItemDTO> votes) {
        this.votes = votes;
    }
}
