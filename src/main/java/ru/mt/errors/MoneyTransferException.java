package ru.mt.errors;

import lombok.Builder;

@Builder
public class MoneyTransferException extends Exception {
    private final String transactionId;

    MoneyTransferException(String transactionId) {
        super();
        this.transactionId = transactionId;
    }

    public MoneyTransferException(String transactionId, String message) {
        super(message);
        this.transactionId = transactionId;
    }
}
