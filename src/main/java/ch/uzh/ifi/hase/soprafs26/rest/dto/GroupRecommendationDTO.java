package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class GroupRecommendationDTO {
    private Long groupId;
    private List<RecommendedMovieDTO> recommendations;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public List<RecommendedMovieDTO> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendedMovieDTO> recommendations) { this.recommendations = recommendations; }
}