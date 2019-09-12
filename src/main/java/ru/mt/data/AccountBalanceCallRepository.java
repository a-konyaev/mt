package ru.mt.data;

import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;

public interface AccountBalanceCallRepository {
    void putNewCall(AccountBalanceCall call);
    AccountBalanceCallResult getCallResult(String callId);

    AccountBalanceCall getNextCall(String accountId);
    void setCallResult(String callId, AccountBalanceCallResult result);
}
