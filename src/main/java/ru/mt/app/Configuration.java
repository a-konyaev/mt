package ru.mt.app;

import lombok.extern.log4j.Log4j2;
import ru.mt.AccountService;
import ru.mt.controller.MoneyTransferController;
import ru.mt.MoneyTransferService;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.data.AccountRepository;
import ru.mt.data.TransactionRepository;
import ru.mt.data.inmemory.InMemoryAccountBalanceCallRepository;
import ru.mt.data.inmemory.InMemoryAccountRepository;
import ru.mt.data.inmemory.InMemoryTransactionRepository;
import ru.mt.errors.ConfigurationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class Configuration {
    private static Map<Class, Component> components = new HashMap<>();

    static {
        initComponents();
    }

    static void run() {
        // do nothing
    }

    /**
     * Components initialization.
     * The order of components initialization should correspond to dependencies between the components.
     */
    private static void initComponents() {
        log.info("Initializing...");

        try {
            // repositories
            components.put(AccountRepository.class, new InMemoryAccountRepository());
            components.put(AccountBalanceCallRepository.class, new InMemoryAccountBalanceCallRepository());
            components.put(TransactionRepository.class, new InMemoryTransactionRepository());

            // services
            components.put(AccountService.class, new AccountService());
            components.put(MoneyTransferService.class, new MoneyTransferService());

            // controller start-up only if not a testing
            components.put(MoneyTransferController.class, new MoneyTransferController());

        } catch (Throwable e) {
            throw new ConfigurationException("Component initialization failed", e);
        }

        log.info("Initialization complete");
    }

    private static void destroyComponents() {
        log.info("Destroying...");

        var hasError = components.values()
                .stream()
                .map(component -> {
                    try {
                        component.destroy();
                        return true;
                    } catch (Throwable e) {
                        log.error("Component destroying failed: " + component.getClass().getCanonicalName(), e);
                        return false;
                    }
                })
                .anyMatch(destroyed -> !destroyed);

        if (hasError) {
            throw new ConfigurationException("Error while destroying components. See errors above.");
        }

        components.clear();

        log.info("Destroyed");
    }

    /**
     * For testing purposes
     */
    public static void reset() {
        destroyComponents();
        initComponents();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getComponent(Class componentClass) throws ConfigurationException {
        Objects.requireNonNull(componentClass, "Component class is null");

        var component = components.get(componentClass);
        if (component == null)
            throw new ConfigurationException("Component not found");

        return (T) component;
    }
}
