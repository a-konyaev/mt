package ru.mt.controller.dto;

import lombok.Getter;

@Getter
public class AccountBalanceResponse extends MoneyTransferResponse {
    private final String accountId;
    private final double balance;

    public AccountBalanceResponse(String accountId, double balance) {
        super(ResponseStatus.OK);

        this.accountId = accountId;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return String.format("OK: accountId = %s; balance = %s", accountId, balance);
    }
}
