package com.example.ebank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Duration;

@Getter
public class TooManyAttemptsException extends ApiException {
    private final Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", "Too many failed attempts. Try again later.");
        this.retryAfter = retryAfter;
    }
}
