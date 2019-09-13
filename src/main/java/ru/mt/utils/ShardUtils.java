package ru.mt.utils;

import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public final class ShardUtils {
    private ShardUtils() {}

    /**
     * Algorithm for getting md5-hash from strings
     */
    private static MessageDigest md5 = getMd5();

    @SneakyThrows
    private static MessageDigest getMd5() {
        return MessageDigest.getInstance("MD5");
    }

    public static int getShardIndexById(String id, int shardCount) {
        byte[] digest = md5.digest(id.getBytes());
        int intValue = ByteBuffer.wrap(digest).getInt();
        int positiveIntValue = intValue & 0x0fffffff;
        return positiveIntValue % shardCount;
    }
}
