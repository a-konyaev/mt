package ru.mt.data.inmemory;

import ru.mt.app.Component;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class InMemoryAccountBalanceCallRepository extends Component implements AccountBalanceCallRepository {

    private final Map<String, AccountBalanceCallAndResult> callByIdMap = new ConcurrentHashMap<>();
    private LinkedBlockingQueue<AccountBalanceCall>[] callQueueArray;

    @Override
    @SuppressWarnings("unchecked")
    public void initShards(int shardCount) {
        if (shardCount <= 0) {
            throw new IllegalArgumentException("shard count must be positive");
        }

        callQueueArray = (LinkedBlockingQueue<AccountBalanceCall>[])
                Array.newInstance(LinkedBlockingQueue.class, shardCount);

        for (int i = 0; i < shardCount; i++) {
            callQueueArray[i] = new LinkedBlockingQueue<>();
        }
    }

    @Override
    public void putNewCall(AccountBalanceCall call, int shardIndex) {
        callByIdMap.put(call.getId(), new AccountBalanceCallAndResult(call));
        callQueueArray[shardIndex].add(call);
    }

    @Override
    public AccountBalanceCallResult getCallResult(String callId, long timeoutMillis) throws InterruptedException {
        var call = findCall(callId);
        return call.waitForResult(timeoutMillis);
    }

    /**
     * Returns next call for an account
     *
     * @param shardIndex The shard index that defines the batch of accounts to process
     * @return The next call or null if there is no new calls
     */
    @Override
    public AccountBalanceCall getNextCall(int shardIndex, long timeoutMillis) throws InterruptedException {
        return callQueueArray[shardIndex].poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setCallResult(String callId, AccountBalanceCallResult result) {
        var call = findCall(callId);
        call.setResult(result);
    }

    private AccountBalanceCallAndResult findCall(String callId) {
        var value = callByIdMap.get(callId);
        if (value == null) {
            throw new IllegalStateException("Call not found: " + callId);
        }

        return value;
    }
}
