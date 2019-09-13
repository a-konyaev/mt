package ru.mt.utils;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@Log4j2
class ShardUtilsTest {

    @Test
    void getShardIndexById() {
        for (int shardCount = 1; shardCount <= 10; shardCount++) {
            for (int i = 0; i < 100; i++) {
                var id = UUID.randomUUID().toString();
                var shardIndex = ShardUtils.getShardIndexById(id, shardCount);
                Assertions.assertTrue(0 <= shardIndex && shardIndex < shardCount);
            }
        }
    }
}