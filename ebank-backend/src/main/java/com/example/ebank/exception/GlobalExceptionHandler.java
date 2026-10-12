package com.example.ebank.exception;

import com.example.ebank.config.AppUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single place that turns exceptions into RFC 7807 problem responses ({@code application/problem+json}).
 * Every response carries a stable {@code code}; internal details (stack traces, SQL) are only logged.
 * Security exceptions raised in the filter chain are routed here as well (see SecurityConfig).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ResponseEntity<ProblemDetail> handleTooManyAttempts(TooManyAttemptsException ex) {
        long seconds = Math.max(1, ex.getRetryAfter().toSeconds() + 1);
        ResponseEntity<ProblemDetail> response = problem(ex.getStatus(), ex.getCode(), ex.getMessage());
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(response.getBody());
    }

    @ExceptionHandler({BadCredentialsException.class, AccountStatusException.class})
    ResponseEntity<ProblemDetail> handleLoginFailure(AuthenticationException ex) {
        // Same answer for unknown user, wrong password and disabled account: no account enumeration.
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        if (ex instanceof CsrfException) {
            return problem(HttpStatus.FORBIDDEN, "CSRF_TOKEN_INVALID", "Missing or invalid CSRF token");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal && principal.passwordChangeRequired()) {
            return problem(HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
                    "You must change your temporary password before continuing");
        }
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to perform this action");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Usually a lost race on a unique constraint that the service pre-checks.
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        body.setProperty("code", "VALIDATION_FAILED");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        // Framework-level errors (malformed JSON, 404 route, 405 method...) get a code too.
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem && problem.getProperties() == null) {
            problem.setProperty("code", HttpStatus.valueOf(statusCode.value()).name());
        }
        return response;
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty("code", code);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
