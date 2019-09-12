package ru.mt.utils;

import lombok.Getter;

public class CountdownTimer {
    @Getter
    private final int timeoutSecs;
    private final long startTime;
    private final long timeoutNanosecs;

    public CountdownTimer(int timeoutSecs) {
        this.startTime = System.nanoTime();
        this.timeoutSecs = timeoutSecs;
        this.timeoutNanosecs = (long)timeoutSecs * 1_000_000_000L;
    }

    public boolean isTimeOver() {
        return (System.nanoTime() - startTime > timeoutNanosecs);
    }
}
