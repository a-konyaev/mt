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

    private Map<String, CallMapValue> callMap = new ConcurrentHashMap<>();

    @RequiredArgsConstructor
    static class CallMapValue {
        final AccountBalanceCall call;
        AccountBalanceCallResult result;

        boolean hasResult() {
            return result != null;
        }
    }

    @Override
    public void putNewCall(AccountBalanceCall call) {
        callMap.put(call.getId(), new CallMapValue(call));
    }

    @Override
    public AccountBalanceCallResult getCallResult(String callId) {
        return null;
    }

    /**
     * Returns next call for account
     * @param accountId Account id
     * @return The next call or null if there is no new calls
     */
    @Override
    public AccountBalanceCall getNextCall(String accountId) {
        return null;
    }

    @Override
    public void setCallResult(String callId, AccountBalanceCallResult result) {
        var value = callMap.get(callId);

        if (value == null) {
            throw new IllegalStateException("The call not found: " + callId);
        }

        if (value.hasResult()) {
            throw new IllegalStateException("The call already has result: " + callId);
        }

        value.result = result;
    }
}
