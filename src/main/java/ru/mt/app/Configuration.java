package ru.mt.app;

import lombok.NonNull;
import ru.mt.AccountService;
import ru.mt.data.AccountRepository;
import ru.mt.data.inmemory.InMemoryAccountRepository;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
    private static Map<Class, Object> beans = new HashMap<>();

    static {
        // the order of beans initialization should correspond to dependencies between the beans:
        // repositories
        beans.put(AccountRepository.class, new InMemoryAccountRepository());
        // services
        beans.put(AccountService.class, new AccountService());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(@NonNull Class<? extends T> beanClass) throws ConfigurationException {
        var bean = beans.get(beanClass);
        if (bean == null)
            throw new ConfigurationException("Bean not found: " + beanClass);

        return (T)bean;
    }
}
