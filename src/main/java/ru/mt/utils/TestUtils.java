package ru.mt.utils;

import lombok.Getter;
import lombok.Setter;

public final class TestUtils {
    private TestUtils() {}

    @Setter
    @Getter
    public static boolean testRegime;

    public static void slowThread() {
        randomSleep(10, 100);
    }

    public static void randomSleep(int fromMillis, int toMillis) {
        try {
            Thread.sleep(RandomUtils.getRandomInt(fromMillis, toMillis));
        } catch (InterruptedException ignored) {
        }
    }
}
