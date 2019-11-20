package ru.mt;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import ru.mt.app.Configuration;
import ru.mt.utils.TestUtils;

@RequiredArgsConstructor
abstract class BaseTest<T> {
    private final Class<? extends T> serviceClass;
    T service;

    @BeforeAll
    static void init() {
        TestUtils.setTestRegime(true);
    }

    @BeforeEach
    void setUp() {
        service = Configuration.getComponent(serviceClass);
    }
}
