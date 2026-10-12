package com.example.ebank.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginThrottleTest {

    /** A clock the test moves by hand. */
    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginThrottle throttle =
            new LoginThrottle(new LoginThrottleProperties(3, 5, Duration.ofMinutes(15)), clock);

    @Test
    void usernameIsBlockedAfterTheLimitUntilTheWindowPasses() {
        for (int i = 0; i < 3; i++) {
            assertThat(throttle.retryAfter("alice", "1.1.1.1")).isZero();
            throttle.recordFailure("alice", "1.1.1.1");
            clock.advance(Duration.ofMinutes(1));
        }
        // Failures at 10:00, 10:01, 10:02; now 10:03 -> blocked until 10:15
        assertThat(throttle.retryAfter("alice", "9.9.9.9")).isEqualTo(Duration.ofMinutes(12));

        clock.advance(Duration.ofMinutes(12));
        assertThat(throttle.retryAfter("alice", "9.9.9.9")).isZero();
    }

    @Test
    void usernameMatchingIsCaseInsensitive() {
        for (int i = 0; i < 3; i++) {
            throttle.recordFailure("Alice@Test", null);
        }
        assertThat(throttle.retryAfter("alice@test", null)).isPositive();
    }

    @Test
    void ipIsBlockedAcrossDifferentUsernames() {
        for (int i = 0; i < 5; i++) {
            throttle.recordFailure("user" + i, "6.6.6.6");
        }
        assertThat(throttle.retryAfter("someone-else", "6.6.6.6")).isPositive();
        assertThat(throttle.retryAfter("someone-else", "7.7.7.7")).isZero();
    }

    @Test
    void successClearsTheUsernameButNotTheIp() {
        for (int i = 0; i < 3; i++) {
            throttle.recordFailure("bob", "2.2.2.2");
        }
        throttle.recordSuccess("bob");
        assertThat(throttle.retryAfter("bob", null)).isZero();
    }
}
