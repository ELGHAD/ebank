package com.example.ebank.dto;

/**
 * @param expiresIn token lifetime in seconds
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
