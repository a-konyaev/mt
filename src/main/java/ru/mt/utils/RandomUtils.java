package ru.mt.utils;

import java.util.Random;

public final class RandomUtils {
    private static final Random RANDOM = new Random();

    private RandomUtils() {
    }

    public static int getRandomInt(int from, int to) {
        return from + (int) (RANDOM.nextFloat() * (to - from + 1));
    }

    public static double getRandomDouble(double from, double to) {
        return from + RANDOM.nextFloat() * (to - from);
    }
}