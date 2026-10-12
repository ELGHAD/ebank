package com.example.ebank.exception;

import org.springframework.http.HttpStatus;

/**
 * The request is well-formed but violates a business rule (insufficient funds, blocked account...): 422.
 */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
