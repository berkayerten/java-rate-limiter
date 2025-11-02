package com.techthrivecatalyst.ratelimiter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FixedWindowStrategy implements LimitTrackingStrategy {

    private final int maxRequests;
    private final long timeWindowMillis;
    private final Map<String, Window> windows;

    public FixedWindowStrategy(int maxRequests, long timeWindowMillis) {
        this.maxRequests = maxRequests;
        this.timeWindowMillis = timeWindowMillis;
        this.windows = new ConcurrentHashMap<>();
    }

    @Override
    public Metadata checkLimitFor(String clientIdentifier) {
        Instant now = Instant.now();
        long windowKey = now.toEpochMilli() / timeWindowMillis;
        Window window = windows.computeIfAbsent(clientIdentifier, k -> new Window());
        synchronized (window) {
            if (window.windowStart != windowKey) {
                window.windowStart = windowKey;
                window.count = 0;
            }

            long nextWindow = (window.windowStart + 1) * timeWindowMillis;
            if (window.count < maxRequests) {
                window.count++;
                return new Metadata(true, maxRequests - window.count, nextWindow);
            }

            return new Metadata(false, 0, nextWindow);
        }
    }

    private static class Window {
        long windowStart;
        int count;
    }
}
