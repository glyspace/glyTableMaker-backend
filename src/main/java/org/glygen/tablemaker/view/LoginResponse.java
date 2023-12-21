package org.glygen.tablemaker.view;

public class LoginResponse {
    
    String token;
    User user;
    
    public LoginResponse() {
        // TODO Auto-generated constructor stub
    }
    
    public LoginResponse(String token, User user) {
        super();
        this.token = token;
        this.user = user;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }
}
