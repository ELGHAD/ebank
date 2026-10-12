package com.example.ebank;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A real PostgreSQL (same major version as Docker Compose) for integration tests.
 * Spring caches the test context, so one container is shared by all integration test classes.
 * <p>
 * Like production, Flyway migrates as the schema owner while the application connects as the restricted
 * {@code ebank_app} role: every integration test therefore also proves the app works with least privilege.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final String APP_USER = "ebank_app";
    public static final String APP_PASSWORD = "ebank_app_test_password";

    @Bean
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18-alpine").withInitScript("db/test-init.sql");
    }

    @Bean
    DynamicPropertyRegistrar databaseProperties(PostgreSQLContainer postgres) {
        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", () -> APP_USER);
            registry.add("spring.datasource.password", () -> APP_PASSWORD);
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
        };
    }
}
