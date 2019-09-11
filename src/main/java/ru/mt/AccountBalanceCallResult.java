package ru.mt;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountBalanceCallResult {
    private Throwable error;
    private double amount;

    public boolean hasError() {
        return error != null;
    }
}
