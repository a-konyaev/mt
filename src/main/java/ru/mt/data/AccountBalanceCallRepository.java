package ru.mt.data;

import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;

/**
 * Repository for AccountBalanceCall entities.
 * The repository is smart because can wait for a next call and a call result.
 */
public interface AccountBalanceCallRepository {

    void initShards(int shardCount);

    void putNewCall(AccountBalanceCall call, int shardIndex);

    AccountBalanceCallResult getCallResult(String callId, long timeoutMillis) throws InterruptedException;

    AccountBalanceCall getNextCall(int shardIndex, long timeoutMillis) throws InterruptedException;

    void setCallResult(String callId, AccountBalanceCallResult result);
}
