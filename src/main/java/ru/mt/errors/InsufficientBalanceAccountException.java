package ru.mt.errors;

import lombok.Getter;

@Getter
public class InsufficientBalanceAccountException extends AccountException {
    private final double availableBalance;
    private final double requestedAmount;

    public InsufficientBalanceAccountException(String accountId, double availableBalance, double requestedAmount) {
        super(accountId);
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }
}
