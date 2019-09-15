package ru.mt.app;

import lombok.extern.log4j.Log4j2;
import ru.mt.AccountService;
import ru.mt.MoneyTransferService;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.data.AccountRepository;
import ru.mt.data.TransactionRepository;
import ru.mt.data.inmemory.InMemoryAccountBalanceCallRepository;
import ru.mt.data.inmemory.InMemoryAccountRepository;
import ru.mt.data.inmemory.InMemoryTransactionRepository;
import ru.mt.utils.Assert;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class Configuration {
    private static Map<Class, Component> components = new HashMap<>();

    static {
        initComponents();
    }

    /**
     * Components initialization.
     * The order of components initialization should correspond to dependencies between the components.
     */
    private static void initComponents() {
        log.debug("Initializing components...");

        // repositories
        components.put(AccountRepository.class, new InMemoryAccountRepository());
        components.put(AccountBalanceCallRepository.class, new InMemoryAccountBalanceCallRepository());
        components.put(TransactionRepository.class, new InMemoryTransactionRepository());

        // services
        components.put(AccountService.class, new AccountService());
        components.put(MoneyTransferService.class, new MoneyTransferService());

        log.debug("Initializing components...done");
    }

    private static void destroyComponents() {
        log.debug("Destroying components...");

        var hasError = components.values()
                .stream()
                .map(component -> {
                    try {
                        component.destroy();
                        return true;
                    } catch (Throwable e) {
                        log.error("Component destroying failed: " + component.getClass().getCanonicalName(), e);
                        return false;
                    } })
                .anyMatch(destroyed -> !destroyed);

        if (hasError) {
            throw new ConfigurationException("Error while destroying components. See errors above.");
        }

        components.clear();

        log.debug("Destroying components...done");
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
        Assert.notNull(componentClass, "Component class is null");

        var component = components.get(componentClass);
        if (component == null)
            throw new ConfigurationException("Component not found");

        return (T) component;
    }
}
