package com.example.ebank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Only used when the "demo" profile is active. */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(String organizationName, String agentUsername, String agentPassword) {
}
