package ru.mt.data.inmemory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;

import java.util.Objects;

@RequiredArgsConstructor
class AccountBalanceCallTableRow {
    private final Object signal = new Object();
    @Getter
    private final AccountBalanceCall call;
    @Getter
    private volatile AccountBalanceCallResult result = null;

    void setResult(AccountBalanceCallResult result) {
        Objects.requireNonNull(result, "AccountBalanceCallResult is null");

        synchronized (signal) {
            if (this.result != null) {
                throw new IllegalStateException("Call already has result: " + result);
            }

            this.result = result;
            signal.notifyAll();
        }
    }

    AccountBalanceCallResult waitForResult(long timeoutMillis) throws InterruptedException {
        synchronized (signal) {
            if (result != null) {
                return result;
            }

            signal.wait(timeoutMillis);
            return result;
        }
    }
}
