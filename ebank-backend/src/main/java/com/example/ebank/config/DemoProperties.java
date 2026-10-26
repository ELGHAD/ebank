package com.example.ebank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Only used when the "demo" profile is active. The agent is required; the organization admin and the platform
 * admin are created only when their passwords are configured.
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(String organizationName,
                             String agentUsername, String agentPassword,
                             String orgAdminUsername, String orgAdminPassword,
                             String platformAdminUsername, String platformAdminPassword) {
}
