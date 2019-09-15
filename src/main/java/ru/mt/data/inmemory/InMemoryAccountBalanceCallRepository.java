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

    private final Map<String, AccountBalanceCallTableRow> callTable = new ConcurrentHashMap<>();
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
        callTable.put(call.getId(), new AccountBalanceCallTableRow(call));
        callQueueArray[shardIndex].add(call);
    }

    @Override
    public AccountBalanceCallResult getCallResult(String callId, long timeoutMillis) throws InterruptedException {
        var row = getCallTableRow(callId);
        return row.waitForResult(timeoutMillis);
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
        var row = getCallTableRow(callId);
        row.setResult(result);
    }

    private AccountBalanceCallTableRow getCallTableRow(String callId) {
        var row = callTable.get(callId);
        if (row == null) {
            throw new IllegalStateException("Call not found: " + callId);
        }

        return row;
    }
}
