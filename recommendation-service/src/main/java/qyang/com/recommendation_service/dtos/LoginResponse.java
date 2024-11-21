package qyang.com.recommendation_service.dtos;

public class LoginResponse {
    private String token;
    private String userId;
    private String username;

    public LoginResponse(String token, String userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
