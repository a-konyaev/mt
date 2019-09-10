package ru.mt.data;

import ru.mt.domain.Account;

import java.util.Set;

public interface AccountRepository {

    Set<String> findAll();

    Account createNew();

}
