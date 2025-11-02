package com.techthrivecatalyst.ratelimiter;

public record Metadata(boolean allowed, int remainingRequests, long retryAfter) {
}
