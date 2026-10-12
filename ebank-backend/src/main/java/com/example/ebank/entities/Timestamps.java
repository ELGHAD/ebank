package com.example.ebank.entities;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Timestamps {

    private Timestamps() {
    }

    /**
     * Current time at PostgreSQL's precision (microseconds), so the value returned right after an insert
     * is identical to the value read back later.
     */
    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
