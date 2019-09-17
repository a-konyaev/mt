package ru.mt.errors;

public class MoneyTransferException extends Exception {
    MoneyTransferException() {
        super();
    }

    public MoneyTransferException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
