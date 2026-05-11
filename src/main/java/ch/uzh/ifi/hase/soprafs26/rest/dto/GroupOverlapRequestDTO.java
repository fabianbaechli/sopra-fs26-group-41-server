package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.Map;

public class GroupOverlapRequestDTO {
    private List<Map<String,Float>> MemberWatchedRatings;


    public GroupOverlapRequestDTO(List<Map<String,Float>> MemberWatchedRatings) {
        this.MemberWatchedRatings = MemberWatchedRatings;

    }

    public List<Map<String,Float>> getMemberWatchedRatings() { return MemberWatchedRatings; }
    public void setMemberWatchedRatings(List<Map<String,Float>> MemberWatchedRatings) { this.MemberWatchedRatings = MemberWatchedRatings; }

}