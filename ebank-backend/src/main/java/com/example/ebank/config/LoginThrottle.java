package com.example.ebank.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for password checks (login and password change).
 * <p>
 * Sliding window of failures, kept per username (protects one account against a distributed attack)
 * and per client IP (protects many accounts against one source). When either limit is reached, further
 * attempts are refused, even with the correct password, until the oldest failure leaves the window.
 * A successful attempt clears the username's failures.
 * <p>
 * State is in memory: it resets on restart and is per instance. A multi-instance deployment would move it
 * to a shared store (database or Redis); that is not needed for a single-instance demo.
 */
@Component
public class LoginThrottle {

    private final LoginThrottleProperties properties;
    private final Clock clock;
    /** Above this many tracked keys, expired entries are purged (bounds memory under a username-spraying attack). */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    @Autowired
    public LoginThrottle(LoginThrottleProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginThrottle(LoginThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** @return zero if the attempt is allowed, otherwise how long to wait */
    public Duration retryAfter(String username, String ip) {
        Instant now = clock.instant();
        Duration byUser = wait(userKey(username), properties.maxFailuresPerUsername(), now);
        Duration byIp = ip == null ? Duration.ZERO : wait(ipKey(ip), properties.maxFailuresPerIp(), now);
        return byUser.compareTo(byIp) >= 0 ? byUser : byIp;
    }

    public void recordFailure(String username, String ip) {
        Instant now = clock.instant();
        if (failures.size() > CLEANUP_THRESHOLD) {
            purgeExpired(now);
        }
        add(userKey(username), now);
        if (ip != null) {
            add(ipKey(ip), now);
        }
    }

    public void recordSuccess(String username) {
        failures.remove(userKey(username));
    }

    private Duration wait(String key, int limit, Instant now) {
        Deque<Instant> window = failures.get(key);
        if (window == null) {
            return Duration.ZERO;
        }
        synchronized (window) {
            prune(window, now);
            if (window.size() < limit) {
                return Duration.ZERO;
            }
            // Allowed again when enough old failures have expired to fall below the limit
            Instant oldestRelevant = window.stream().skip(window.size() - limit).findFirst().orElseThrow();
            return Duration.between(now, oldestRelevant.plus(properties.window()));
        }
    }

    private void add(String key, Instant now) {
        Deque<Instant> window = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            prune(window, now);
            window.addLast(now);
        }
    }

    private void purgeExpired(Instant now) {
        failures.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }

    private void prune(Deque<Instant> window, Instant now) {
        Instant cutoff = now.minus(properties.window());
        while (!window.isEmpty() && !window.peekFirst().isAfter(cutoff)) {
            window.pollFirst();
        }
    }

    private static String userKey(String username) {
        return "user:" + username.trim().toLowerCase(Locale.ROOT);
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }
}
