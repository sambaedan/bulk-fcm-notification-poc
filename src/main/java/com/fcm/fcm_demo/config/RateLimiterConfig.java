package com.fcm.fcm_demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class RateLimiterConfig {

    @Bean
    public SimpleRateLimiter simpleRateLimiter(RateProperties props) {
        return new SimpleRateLimiter(props.getMaxRequestsPerSecond(), props.getBurstCapacity());
    }

    public static class SimpleRateLimiter {
        private final long capacity;
        private final long refillPerSecond;
        private final AtomicLong tokens = new AtomicLong(0);
        private volatile long lastRefillNanos = System.nanoTime();

        public SimpleRateLimiter(long maxPerSec, long burstCapacity) {
            this.capacity = burstCapacity;
            this.refillPerSecond = maxPerSec;
        }

        public boolean tryAcquire(long permits) {
            refill();
            long current = tokens.get();
            if (current >= permits) {
                return tokens.compareAndSet(current, current - permits);
            }
            return false;
        }

        public void awaitAcquire(long permits) throws InterruptedException {
            while (!tryAcquire(permits)) {
                Thread.sleep(1);
            }
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillNanos;
            if (elapsedNanos >= Duration.ofMillis(10).toNanos()) {
                long add = (refillPerSecond * elapsedNanos) / 1_000_000_000L;
                long updated = Math.min(capacity, tokens.get() + add);
                tokens.set(updated);
                lastRefillNanos = now;
            }
        }
    }

    @Bean
    public RateProperties rateProperties(org.springframework.core.env.Environment env) {
        RateProperties p = new RateProperties();
        p.setMaxRequestsPerSecond(Long.parseLong(env.getProperty("rate.max-requests-per-second", "8000")));
        p.setBurstCapacity(Long.parseLong(env.getProperty("rate.burst-capacity", "9000")));
        return p;
    }

    @Setter
    @Getter
    public static class RateProperties {
        private long maxRequestsPerSecond;
        private long burstCapacity;

    }
}
