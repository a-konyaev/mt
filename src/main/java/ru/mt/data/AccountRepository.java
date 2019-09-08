package ru.mt.data;

import java.util.Set;

public interface AccountRepository {

    Set<String> findAll();

    String createNew();

}
