package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class GroupsGetResponseDTO {
    private List<GroupSummaryDTO> groups;

    public List<GroupSummaryDTO> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupSummaryDTO> groups) {
        this.groups = groups;
    }
}