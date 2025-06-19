package com.plagiarism.detector.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private User user;
    private String message;
    private boolean success;
    
    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
        this.success = true;
    }
    
    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
    
    // Legacy constructor for backward compatibility
    public AuthResponse(String token, String username, String email) {
        this.token = token;
        this.user = new User();
        this.user.setUsername(username);
        this.user.setEmail(email);
        this.success = true;
    }
} 