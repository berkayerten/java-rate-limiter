package com.techthrivecatalyst.ratelimiter;

public class RateLimiter {
    private final LimitTrackingStrategy limitTrackingStrategy;

    public RateLimiter(LimitTrackingStrategy limitTrackingStrategy) {
        this.limitTrackingStrategy = limitTrackingStrategy;
    }

    public Metadata isLimitReached(String clientIdentifier) {
        return limitTrackingStrategy.checkLimitFor(clientIdentifier);
    }
}
