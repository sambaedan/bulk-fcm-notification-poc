package com.fcm.fcm_demo.service;

import java.util.concurrent.ThreadLocalRandom;

public class RetryPolicy {
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final boolean jitter;

    public RetryPolicy(int maxAttempts, long initialBackoffMs, long maxBackoffMs, boolean jitter) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
        this.jitter = jitter;
    }

    public long backoff(int attempt) {
        long exp = initialBackoffMs * (1L << Math.min(attempt - 1, 10));
        long delay = Math.min(exp, maxBackoffMs);
        if (jitter) {
            long delta = (long) (delay * 0.2);
            long low = Math.max(0, delay - delta);
            long high = delay + delta;
            return ThreadLocalRandom.current().nextLong(low, high + 1);
        }
        return delay;
    }

    public int maxAttempts() {
        return maxAttempts;
    }
}

