package ru.mt.errors;

public class AccountNotExistException extends AccountException {
    public AccountNotExistException(String accountId) {
        super(accountId);
    }
}
