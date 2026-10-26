package com.example.ebank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A plan limit was reached (422 QUOTA_EXCEEDED, with the quota, limit and plan in the problem body). */
@Getter
public class QuotaExceededException extends ApiException {
    private final String quota;
    private final int limit;
    private final String plan;

    public QuotaExceededException(String quota, String quotaLabel, int limit, String plan) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "QUOTA_EXCEEDED",
                "Your bank's " + plan + " plan allows " + limit + " " + quotaLabel
                        + ". Ask your bank administrator to upgrade the plan.");
        this.quota = quota;
        this.limit = limit;
        this.plan = plan;
    }
}
