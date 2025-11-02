package com.techthrivecatalyst.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowStrategy implements LimitTrackingStrategy {

    private final int maxRequests;
    private final long timeWindowMillis;
    private Map<String, ArrayDeque<Instant>> requestTimestamps;

    public SlidingWindowStrategy(int maxRequests, long timeWindowMillis) {
        this.maxRequests = maxRequests;
        this.timeWindowMillis = timeWindowMillis;
        this.requestTimestamps = new ConcurrentHashMap<>();
    }

    @Override
    public Metadata checkLimitFor(String clientIdentifier) {
        ArrayDeque<Instant> timestamps = requestTimestamps.computeIfAbsent(clientIdentifier, _ -> new ArrayDeque<>());
        Instant now = Instant.now();
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && Duration.between(timestamps.peekFirst(), now).compareTo(Duration.ofMillis(timeWindowMillis)) > 0) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                long resetTime = timestamps.isEmpty()
                        ? now.toEpochMilli() + timeWindowMillis
                        : timestamps.peekFirst().toEpochMilli() + timeWindowMillis;
                int remaining = maxRequests - timestamps.size();
                return new Metadata(true, remaining, resetTime);
            } else {
                long resetTime = timestamps.peekFirst().toEpochMilli() + timeWindowMillis;
                return new Metadata(false, 0, resetTime);
            }
        }
    }
}
