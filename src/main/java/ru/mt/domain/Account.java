package ru.mt.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class Account {
    /**
     * Идентификатор счета
     */
    @Getter
    private final String id;
    /**
     * Сумма денег на счете, т.е. его текущий баланс
     */
    @Getter
    @Setter
    double amount;
}
