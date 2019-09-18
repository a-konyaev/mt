package ru.mt.domain;

import lombok.*;
import ru.mt.utils.TimeUtils;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Транзакция выполнения операции перевода денег с одного счета на другой
 */
@Getter
@ToString
public class Transaction {
    private final String id = UUID.randomUUID().toString();
    private final long ts = TimeUtils.getTimestamp();

    private final String accountIdFrom;
    private final String accountIdTo;
    private final BigDecimal amount;
    private final TransactionStatus status;

    public Transaction(String accountIdFrom, String accountIdTo, BigDecimal amount, TransactionStatus status) {
        this.accountIdFrom = accountIdFrom;
        this.accountIdTo = accountIdTo;
        this.amount = amount;
        this.status = status;
    }
}
