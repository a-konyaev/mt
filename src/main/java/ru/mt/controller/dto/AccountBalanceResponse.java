package ru.mt.controller.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class AccountBalanceResponse extends MoneyTransferResponse {
    private final String accountId;
    private final BigDecimal balance;

    public AccountBalanceResponse(String accountId, BigDecimal balance) {
        super(ResponseStatus.OK);

        this.accountId = accountId;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return String.format("OK: accountId = %s; balance = %s", accountId, balance);
    }
}
