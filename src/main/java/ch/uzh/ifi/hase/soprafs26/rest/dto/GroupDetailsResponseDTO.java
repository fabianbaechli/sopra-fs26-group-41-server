package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class GroupDetailsResponseDTO {
    private Long id;
    private String name;
    private Long ownerId;
    private String joinUrl;
    private String groupProfilePicture;
    private List<GroupMemberDTO> members;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
    }

    public String getGroupProfilePicture() {
        return groupProfilePicture;
    }

    public void setGroupProfilePicture(String groupProfilePicture) {
        this.groupProfilePicture = groupProfilePicture;
    }

    public List<GroupMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberDTO> members) {
        this.members = members;
    }
}