package com.example.ebank.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.login-throttle")
public record LoginThrottleProperties(
        @Min(1) int maxFailuresPerUsername,
        @Min(1) int maxFailuresPerIp,
        @NotNull Duration window) {
}
