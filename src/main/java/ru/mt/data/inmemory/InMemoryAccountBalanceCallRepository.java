package ru.mt.data.inmemory;

import lombok.RequiredArgsConstructor;
import ru.mt.app.Component;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountBalanceCallRepository
        extends Component
        implements AccountBalanceCallRepository {

    private Map<String, CallWithResult> callMap = new ConcurrentHashMap<>();

    @RequiredArgsConstructor
    private static class CallWithResult {
        final AccountBalanceCall call;
        AccountBalanceCallResult result;

        boolean hasResult() {
            return result != null;
        }
    }

    @Override
    public void putNewCall(AccountBalanceCall call) {
        callMap.put(call.getId(), new CallWithResult(call));
    }

    @Override
    public AccountBalanceCallResult getCallResult(String callId) {
        var callWithResult = findCall(callId);
        return callWithResult.result;
    }

    /**
     * Returns next call for an account
     * @param shardIndex The shard index that defines the batch of accounts to process
     * @return The next call or null if there is no new calls
     */
    @Override
    public AccountBalanceCall getNextCall(int shardIndex) {
        //todo
        return null;
    }

    @Override
    public void setCallResult(String callId, AccountBalanceCallResult result) {
        var callWithResult = findCall(callId);
        if (callWithResult.hasResult()) {
            throw new IllegalStateException("Call already has result: " + callId);
        }

        callWithResult.result = result;
    }

    private CallWithResult findCall(String callId) {
        var value = callMap.get(callId);
        if (value == null) {
            throw new IllegalStateException("Call not found: " + callId);
        }

        return value;
    }
}
