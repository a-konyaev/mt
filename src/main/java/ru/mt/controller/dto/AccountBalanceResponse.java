package ru.mt.controller.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class AccountBalanceResponse extends MoneyTransferResponse {
    private final String accountId;
    private final String balance;

    public AccountBalanceResponse(String accountId, BigDecimal balance) {
        super(ResponseStatus.OK);

        this.accountId = accountId;
        this.balance = balance.setScale(2, RoundingMode.UP).toString();
    }

    @Override
    public String toString() {
        return String.format("OK: accountId = %s; balance = %s", accountId, balance);
    }
}
