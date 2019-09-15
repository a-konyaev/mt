package ru.mt;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void tearDown() {
        //todo: reset лучше вызывать в конкретном тесте, если ему нужно, т.к. если вызывать его постоянно,
        // то время выполнения всех тестов сильно увеличивается из-за того, что системе требуется ~1 сек на остановку
        //Configuration.reset();
    }
}
