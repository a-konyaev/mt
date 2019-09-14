package ru.mt.data.inmemory;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;

@RequiredArgsConstructor
class AccountBalanceCallAndResult {
    private final Object resultSignal = new Object();
    @Getter
    private final AccountBalanceCall call;
    @Getter
    private volatile AccountBalanceCallResult result;

    void setResult(@NonNull AccountBalanceCallResult result) {
        synchronized (resultSignal) {
            if (this.result != null) {
                throw new IllegalStateException("Call already has result: " + result);
            }

            this.result = result;
            resultSignal.notifyAll();
        }
    }

    AccountBalanceCallResult waitForResult(long timeoutMillis) throws InterruptedException {
        synchronized (resultSignal) {
            if (result != null)
                return result;

            resultSignal.wait(timeoutMillis);
            return result;
        }
    }
}
