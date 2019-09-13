package ru.mt.utils;

import lombok.Getter;

public class CountdownTimer {
    @Getter
    private final int timeoutMillis;
    private final long startTime;
    private final long timeoutNanos;

    public CountdownTimer(int timeoutMillis) {
        this.startTime = System.nanoTime();
        this.timeoutMillis = timeoutMillis;
        this.timeoutNanos = (long)timeoutMillis * 1_000_000L;
    }

    public boolean isTimeOver() {
        return (System.nanoTime() - startTime > timeoutNanos);
    }
}
