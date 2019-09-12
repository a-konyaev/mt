package ru.mt.data.inmemory;

import ru.mt.app.Component;
import ru.mt.data.AccountRepository;
import ru.mt.domain.Account;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountRepository
        extends Component
        implements AccountRepository {

    private Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Set<String> findAll() {
        // makes a copy to prevent reflection of the accounts map's changes in this set of keys
        return new HashSet<>(accounts.keySet());
    }

    @Override
    public Account createNew() {
        var account = new Account(UUID.randomUUID().toString());
        accounts.put(account.getId(), account);
        return account;
    }
}
