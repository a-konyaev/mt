package ru.mt;

import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.data.AccountRepository;
import ru.mt.domain.Account;
import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;
import ru.mt.utils.CountdownTimer;
import ru.mt.utils.ShardUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Сервис, через который выполняются все операции со счетами.
 * Также отвечает за:
 * - создание AccountBalanceManager-ов и распределение между ними счетов
 * - перебалансировку AccountBalanceManager-ов в случаях, когда какие то выходят из строя, или наоборот создаются новые
 */
@Log4j2
public class AccountService extends Component {
    /**
     * Кол-во шард/партиций или пачек, на которые распределяются все счета системы.
     * Каждую пачку счетов будет обрабатывать выделенный экземпляр AccountBalanceManager.
     * todo: получать значение из параметров или выставлять автоматически = кол-во ядер CPU
     * но пока =1, т.к. требуется более тщательная проверка тестами.
     */
    private static final int SHARD_COUNT = 1;
    private final AccountRepository accountRepo;
    private final AccountBalanceCallRepository balanceCallRepo;
    private final List<AccountBalanceManager> accountBalanceManagers = new ArrayList<>();

    public AccountService() {
        accountRepo = Configuration.getComponent(AccountRepository.class);

        balanceCallRepo = Configuration.getComponent(AccountBalanceCallRepository.class);
        balanceCallRepo.initShards(SHARD_COUNT);

        for (int i = 0; i < SHARD_COUNT; i++) {
            accountBalanceManagers.add(new AccountBalanceManager(i));
        }
    }

    @Override
    protected void destroyInternal() {
        accountBalanceManagers.forEach(AccountBalanceManager::destroy);
    }

    /**
     * Get all accounts identifiers
     *
     * @return set of account identifiers
     */
    Set<String> getAccounts() {
        return accountRepo.findAllAccount();
    }

    /**
     * Create new account
     *
     * @return the created account id
     */
    String createNewAccount() {
        var id = UUID.randomUUID().toString();
        var account = new Account(id);
        accountRepo.saveNewAccount(account);
        return id;
    }

    /**
     * Available account balance = current account balance minus all reserved amounts
     *
     * @param accountId the account id
     * @return Available account balance
     */
    AccountBalanceCallResult getAccountBalance(String accountId) {
        var call = AccountBalanceCall.getAvailableBalance(accountId);
        return executeCall(call);
    }

    /**
     * Increase account balance by the amount
     *
     * @param accountId     the account id
     * @param transactionId the transaction in which the operation is performed
     * @param amount        amount by which the balance will be increased
     */
    AccountBalanceCallResult addAmount(String accountId, String transactionId, BigDecimal amount) {
        var call = AccountBalanceCall.addAmount(accountId, transactionId, amount);
        return executeCall(call);
    }

    /**
     * Reserve the amount on the account balance
     *
     * @param accountId     the account id
     * @param transactionId the transaction in which the operation is performed
     * @param amount        amount to reserve
     */
    AccountBalanceCallResult reserveAmount(String accountId, String transactionId, BigDecimal amount) {
        var call = AccountBalanceCall.reserveAmount(accountId, transactionId, amount);
        return executeCall(call);
    }

    /**
     * Debit the account for the amount that was early reserved
     *
     * @param accountId     the account id
     * @param transactionId the transaction in which the operation is performed
     */
    AccountBalanceCallResult debitReservedAmount(String accountId, String transactionId) {
        var call = AccountBalanceCall.debitReservedAmount(accountId, transactionId);
        return executeCall(call);
    }

    AccountBalanceCallResult cancelReservedAmount(String accountId, String transactionId) {
        var call = AccountBalanceCall.cancelReservedAmount(accountId, transactionId);
        return executeCall(call);
    }

    //region Balance calls execution

    /**
     * синхронно выполняет "вызов", после чего возвращает результат выполнения
     */
    private AccountBalanceCallResult executeCall(AccountBalanceCall call) {
        log.debug("executing the call: " + call);
        putNewCall(call);
        var result = waitForCallResult(call.getId());
        log.debug("call executing done. result: " + result);
        return result;
    }

    private void putNewCall(AccountBalanceCall call) {
        var shardIndex = ShardUtils.getShardIndexById(call.getAccountId(), SHARD_COUNT);
        balanceCallRepo.putNewCall(call, shardIndex);
    }

    /**
     * Max waiting call result timeout (60 sec)
     */
    private static final int CALL_RESULT_WAITING_TIMEOUT = 60_000;

    private AccountBalanceCallResult waitForCallResult(String callId) {
        // todo: подумать, как лучше реализовать ожидание, например, через аналог корутин (Quasar, Loom)
        // todo: сделать сервис, который проставляет результат "Ошибка" для вызовов, которые так и не были обработаны.
        log.debug("start waiting result for call: " + callId);

        CountdownTimer timer = new CountdownTimer(CALL_RESULT_WAITING_TIMEOUT);

        while (!Thread.currentThread().isInterrupted() && !isDestroying()) {
            log.debug("checking result for call: " + callId);

            try {
                var result = balanceCallRepo.getCallResult(callId, 1000);
                if (result != null) {
                    return result;
                }
            } catch (InterruptedException e) {
                break;
            }

            if (timer.isTimeOver()) {
                return AccountBalanceCallResult.builder()
                        .callId(callId)
                        .errorMessage("Call result not received in an appropriate time")
                        .build();
            }
        }

        throw new RuntimeException("Waiting for call result was interrupted: " + callId);
    }

    //endregion
}
