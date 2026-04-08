package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GroupSummaryDTO {
    private Long id;
    private String name;
    private String groupProfilePicture;
    private Integer memberCount;

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

    public String getGroupProfilePicture() {
        return groupProfilePicture;
    }

    public void setGroupProfilePicture(String groupProfilePicture) {
        this.groupProfilePicture = groupProfilePicture;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}