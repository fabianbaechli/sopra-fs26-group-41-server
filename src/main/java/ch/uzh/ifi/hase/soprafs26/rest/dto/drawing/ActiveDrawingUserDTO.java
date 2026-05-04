package ch.uzh.ifi.hase.soprafs26.rest.dto.drawing;

public class ActiveDrawingUserDTO {
    private Long userId;
    private String username;

    public ActiveDrawingUserDTO() {
    }

    public ActiveDrawingUserDTO(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}