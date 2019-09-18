package ru.mt;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import ru.mt.app.Configuration;

@RequiredArgsConstructor
abstract class BaseTest<T> {
    private final Class<? extends T> serviceClass;
    T service;

    @BeforeEach
    void setUp() {
        service = Configuration.getComponent(serviceClass);
    }
}
