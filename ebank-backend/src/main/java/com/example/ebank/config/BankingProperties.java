package com.example.ebank.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.banking")
public record BankingProperties(
        @Pattern(regexp = "[A-Z]{3}") String defaultCurrency,
        @NotEmpty List<String> supportedCurrencies) {
}
