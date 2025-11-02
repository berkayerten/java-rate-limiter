package com.techthrivecatalyst.ratelimiter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TokenBucketStrategy implements LimitTrackingStrategy {

    private final int maxRequests;
    private final long timeWindowMillis;
    private final AtomicInteger currentTokens = new AtomicInteger(0);
    private final ScheduledExecutorService reseter;

    public TokenBucketStrategy(int maxRequests, long timeWindowMillis) {
        this.maxRequests = maxRequests;
        this.timeWindowMillis = timeWindowMillis;

        this.reseter = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "token-bucket-reseter");
            thread.setDaemon(true);
            return thread;
        });
        this.reseter.scheduleAtFixedRate(this::resetToken, 0, timeWindowMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Metadata checkLimitFor(String clientIdentifier) {
        long remainingTime = Instant.now().plus(timeWindowMillis, ChronoUnit.MILLIS).toEpochMilli();
        if (currentTokens.get() < maxRequests) {
            int latest = currentTokens.incrementAndGet();
            return new Metadata(true, maxRequests - latest, remainingTime);
        } else {
            return new Metadata(false, 0, remainingTime);
        }
    }

    private void resetToken() {
        currentTokens.set(0);
    }
}
