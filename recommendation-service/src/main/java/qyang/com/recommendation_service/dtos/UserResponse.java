package qyang.com.recommendation_service.dtos;

public class UserResponse {
    private String userId;
    private String username;

    public UserResponse() {}

    public UserResponse(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
