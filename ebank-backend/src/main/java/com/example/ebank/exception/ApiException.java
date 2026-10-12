package com.example.ebank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors. The message is safe to show to API clients;
 * the code is a stable machine-readable identifier.
 */
@Getter
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
