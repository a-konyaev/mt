package ru.mt.errors;

import lombok.Getter;

@Getter
public class AccountException extends RuntimeException {
    private final String accountId;

    public AccountException(String accountId) {
        super();
        this.accountId = accountId;
    }

    public AccountException(String accountId, String message) {
        super(message);
        this.accountId = accountId;
    }

    public AccountException(String accountId, String message, Throwable cause) {
        super(message, cause);
        this.accountId = accountId;
    }
}
