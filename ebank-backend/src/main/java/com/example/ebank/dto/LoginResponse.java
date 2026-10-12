package com.example.ebank.dto;

/**
 * The token itself is not in the body: it is set as an HttpOnly cookie, out of reach of page scripts.
 *
 * @param expiresIn session lifetime in seconds
 */
public record LoginResponse(long expiresIn, UserResponse user) {
}
