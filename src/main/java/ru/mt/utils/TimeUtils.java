package ru.mt.utils;

import java.time.Instant;

public final class TimeUtils {
    private TimeUtils() {}

    public static long getTimestamp() {
        return Instant.now().toEpochMilli();
    }
}
