package ru.mt.errors;

public class MoneyTransferValidationException extends MoneyTransferException {
    public MoneyTransferValidationException(String message) {
        super(message);
    }
}
