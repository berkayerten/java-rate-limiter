package com.techthrivecatalyst.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class RateLimiterTokenBucketTest {

    @Test
    void shouldAllowRequestsWithinSlidingTime() {
        RateLimiter rateLimiter = new RateLimiter(new TokenBucketStrategy(2, 1_000));

        Metadata call1 = rateLimiter.isLimitReached("userId1");
        Metadata call2 = rateLimiter.isLimitReached("userId1");

        assertThat(call1.remainingRequests()).isEqualTo(1);
        assertThat(call1.allowed()).isTrue();
        assertThat(call2.remainingRequests()).isZero();
        assertThat(call2.allowed()).isTrue();
        assertThat(call2.retryAfter() - Instant.now().toEpochMilli()).isLessThanOrEqualTo(1_000L);
    }

    @Test
    void shouldNotAllowRequestsBeyondFixedTimeWindow() throws InterruptedException {
        // given
        RateLimiter rateLimiter = new RateLimiter(new TokenBucketStrategy(2, 5_000));
        Runnable callingRunnable = () -> rateLimiter.isLimitReached("userId1");
        ThreadRunner.runThreads(
                new Thread(callingRunnable),
                new Thread(callingRunnable),
                new Thread(callingRunnable)
        );

        // when then
        Thread.sleep(200L);
        Metadata call = rateLimiter.isLimitReached("userId1");
        assertThat(call.remainingRequests()).isZero();
        assertThat(call.allowed()).isFalse();
    }

    @Test
    void shouldAllowRequestsAfterFixedTimeWindow() throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiter(new TokenBucketStrategy(2, 1_000));

        Metadata call1 = rateLimiter.isLimitReached("userId1");
        Metadata call2 = rateLimiter.isLimitReached("userId1");
        Thread.sleep(1001L);
        Metadata call3 = rateLimiter.isLimitReached("userId1");

        assertThat(call1.remainingRequests()).isEqualTo(1);
        assertThat(call1.allowed()).isTrue();
        assertThat(call2.remainingRequests()).isZero();
        assertThat(call2.allowed()).isTrue();
        assertThat(call3.remainingRequests()).isEqualTo(1);
        assertThat(call3.allowed()).isTrue();
    }

    @Test
    void shouldBeCapableOfHandlingBurstFromBothWindows() throws InterruptedException {
        RateLimiter rateLimiter = new RateLimiter(new TokenBucketStrategy(2, 1_000));

        Instant start = Instant.now();
        Thread.sleep(500L);
        Metadata call1 = rateLimiter.isLimitReached("userId1");
        Metadata call2 = rateLimiter.isLimitReached("userId1");
        Thread.sleep(300L);
        Metadata call3 = rateLimiter.isLimitReached("userId1");
        Metadata call4 = rateLimiter.isLimitReached("userId1");
        Instant end = Instant.now();

        assertThat(call1.remainingRequests()).isEqualTo(1);
        assertThat(call1.allowed()).isTrue();
        assertThat(call2.remainingRequests()).isZero();
        assertThat(call2.allowed()).isTrue();
        assertThat(call3.remainingRequests()).isZero();
        assertThat(call3.allowed()).isFalse();
        assertThat(call4.remainingRequests()).isZero();
        assertThat(call4.allowed()).isFalse();
        assertThat(Duration.between(start, end)).isLessThan(Duration.ofMillis(1_000));
    }
}