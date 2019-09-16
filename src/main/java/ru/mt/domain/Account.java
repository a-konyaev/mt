package ru.mt.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class Account {
    private final String id;

    //todo: тип Double не лучший вариант для работы с деньгами, см. BigDecimal и Currency
    @Setter
    private double balance;
}
