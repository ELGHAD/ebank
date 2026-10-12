package com.example.ebank.transfer;

public enum LedgerEntryType {
    /** Simulated funds entering the system (e.g. the opening deposit). Not linked to a transfer. */
    DEPOSIT,
    /** Money leaving the account as part of a transfer. */
    DEBIT,
    /** Money arriving on the account as part of a transfer. */
    CREDIT
}
