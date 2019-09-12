package ru.mt;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import ru.mt.app.Configuration;

@RequiredArgsConstructor
public abstract class BaseTest<T> {
    private final Class<? extends T> serviceClass;
    protected T service;

    @BeforeEach
    void setUp() {
        service = Configuration.getComponent(serviceClass);
    }

    @AfterEach
    void tearDown() {
        Configuration.reset();
    }
}
