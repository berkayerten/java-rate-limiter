package com.techthrivecatalyst.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class RateLimiterFixedWindowTest {

    @Test
    void shouldAllowRequestsWithinFixedTimeWindow() {
        RateLimiter rateLimiter = new RateLimiter(new FixedWindowStrategy(2, 1_000));

        Metadata call1 = rateLimiter.isLimitReached("userId1");
        Metadata call2 = rateLimiter.isLimitReached("userId1");

        assertThat(call1.remainingRequests()).isEqualTo(1);
        assertThat(call1.allowed()).isTrue();
        assertThat(call2.remainingRequests()).isEqualTo(0);
        assertThat(call2.allowed()).isTrue();
        assertThat(call2.retryAfter() - Instant.now().toEpochMilli()).isLessThan(1_000L);
    }

    @Test
    void shouldNotAllowRequestsBeyondFixedTimeWindow() throws InterruptedException {
        // given
        RateLimiter rateLimiter = new RateLimiter(new FixedWindowStrategy(2, 5_000));
        Runnable callingRunnable = () -> rateLimiter.isLimitReached("userId1");
        ThreadRunner.runThreads(
                new Thread(callingRunnable),
                new Thread(callingRunnable),
                new Thread(callingRunnable)
        );

        // when then
        Thread.sleep(200L);
        Metadata exceedingCall = rateLimiter.isLimitReached("userId1");
        assertThat(exceedingCall.remainingRequests()).isEqualTo(0);
        assertThat(exceedingCall.allowed()).isFalse();
    }

    @Test
    void shouldAllowRequestsAfterFixedTimeWindow() throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiter(new FixedWindowStrategy(2, 1_000));

        Metadata call1 = rateLimiter.isLimitReached("userId1");
        Metadata call2 = rateLimiter.isLimitReached("userId1");
        Thread.sleep(1000L);
        Metadata call3 = rateLimiter.isLimitReached("userId1");
        assertThat(call1.remainingRequests()).isEqualTo(1);
        assertThat(call1.allowed()).isTrue();
        assertThat(call2.remainingRequests()).isEqualTo(0);
        assertThat(call2.allowed()).isTrue();
        assertThat(call3.remainingRequests()).isEqualTo(1);
        assertThat(call3.allowed()).isTrue();
    }

    @Test
    void shouldNotBeCapableOfHandlingBurstFromBothWindows() throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiter(new FixedWindowStrategy(2, 1_000));

        Thread.sleep(500L);
        Instant start = Instant.now();
        Metadata call1 = rateLimiter.isLimitReached("userId1");
        Metadata call2 = rateLimiter.isLimitReached("userId1");
        Thread.sleep(500L);
        Metadata call3 = rateLimiter.isLimitReached("userId1");
        Metadata call4 = rateLimiter.isLimitReached("userId1");
        Instant end = Instant.now();
        System.out.println("Duration: " + Duration.between(start, end).toMillis() + " ms");

        assertThat(call1.remainingRequests()).isEqualTo(1);
        assertThat(call1.allowed()).isTrue();
        assertThat(call2.remainingRequests()).isEqualTo(0);
        assertThat(call2.allowed()).isTrue();
        assertThat(call3.remainingRequests()).isEqualTo(1);
        assertThat(call3.allowed()).isTrue();
        assertThat(call4.remainingRequests()).isEqualTo(0);
        assertThat(call4.allowed()).isTrue();
        assertThat(Duration.between(start, end)).isLessThan(Duration.ofMillis(1_000));
    }
}