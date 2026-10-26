package com.example.ebank.billing;

/** What a plan limits. */
public enum Quota {
    STAFF("staff members"),
    CLIENTS("clients"),
    ACCOUNTS("open accounts"),
    TRANSFERS_PER_MONTH("transfers this month");

    private final String label;

    Quota(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
