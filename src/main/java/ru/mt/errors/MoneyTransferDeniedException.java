package ru.mt.errors;

public class MoneyTransferDeniedException extends MoneyTransferException {
    public MoneyTransferDeniedException(String transactionId) {
        super(transactionId);
    }
}
