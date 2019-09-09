package ru.mt.domain;

import lombok.Builder;

@Builder
public class Account {
    /**
     * Идентификатор счета
     */
    String id;
    /**
     * Сумма денег на счете, т.е. его текущий баланс
     */
    double amount;
}
