package ru.mt.utils;

public final class Assert {
    private Assert() {}

    public static void notNull(Object obj, String errorMessage) {
        if (obj == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void notEmpty(String testStr, String errorMessage) {
        if (testStr == null || testStr.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void positive(double value, String errorMessage) {
        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
