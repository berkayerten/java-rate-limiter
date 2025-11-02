package com.techthrivecatalyst.ratelimiter;

public interface LimitTrackingStrategy {

    Metadata checkLimitFor(String clientIdentifier);
}
