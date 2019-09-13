package ru.mt;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.data.AccountRepository;
import ru.mt.domain.AccountBalanceCall;
import ru.mt.domain.AccountBalanceCallResult;
import ru.mt.errors.AccountException;
import ru.mt.utils.CountdownTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Сервис, через который выполняются все операции со счетами.
 * <p>
 * Также отвечает за
 * - создание AccountBalanceManager-ов и распределение между ними счетов
 * - перебалансировку AccountBalanceManager-ов в случаях, когда какие то выходят из строя, или наоборот создаются новые
 */
@Log4j2
public class AccountService
        extends Component {

    private final AccountRepository accountRepo;
    private final AccountBalanceCallRepository balanceCallRepo;

    public AccountService() {
        accountRepo = Configuration.getComponent(AccountRepository.class);
        balanceCallRepo = Configuration.getComponent(AccountBalanceCallRepository.class);

        initAccountBalanceManagers();
    }

    @Override
    public void destroy() {
        destroyAccountBalanceManagers();
    }

    /**
     * Get all accounts identifiers
     *
     * @return set of account identifiers
     */
    public Set<String> getAccounts() {
        return accountRepo.findAll();
    }

    /**
     * Create new account
     *
     * @return the created account id
     */
    public String createNewAccount() {
        return accountRepo.createNew().getId();
    }

    /**
     * Get available account balance
     *
     * @param accountId the account id
     * @return current account balance minus all reserved amounts
     */
    public double getAccountBalance(@NonNull String accountId) {
        var call = AccountBalanceCall.getBalance(accountId);
        var result = executeCall(call);
        return result.getAmount();
    }

    /**
     * Increase account balance by the amount
     *
     * @param accountId     the account id
     * @param transactionId the transaction in which the operation is performed
     * @param amount        amount by which the balance will be increased
     */
    public void addAmount(@NonNull String accountId, @NonNull String transactionId, double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be positive! amount = " + amount);

        var call = AccountBalanceCall.addAmount(accountId, transactionId, amount);
        executeCall(call);
    }

    /**
     * Reserve the amount on the account balance
     *
     * @param accountId     the account id
     * @param transactionId the transaction in which the operation is performed
     * @param amount        amount to reserve
     */
    public void reserveAmount(@NonNull String accountId, @NonNull String transactionId, double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be positive! amount = " + amount);

        var call = AccountBalanceCall.reserveAmount(accountId, transactionId, amount);
        executeCall(call);
    }

    /**
     * Debit the account for the amount that was early reserved
     *
     * @param accountId     the account id
     * @param transactionId the transaction in which the operation is performed
     */
    public void debitReservedAmount(@NonNull String accountId, @NonNull String transactionId) {
        var call = AccountBalanceCall.debitReservedAmount(accountId, transactionId);
        executeCall(call);
    }

    //region Balance managers

    private final List<AccountBalanceManager> accountBalanceManagers = new ArrayList<>();

    private void initAccountBalanceManagers() {
        // todo: пока что реализация с одним баланс-менеджером
        accountBalanceManagers.add(new AccountBalanceManager(0));
    }

    private void destroyAccountBalanceManagers() {
        accountBalanceManagers.forEach(AccountBalanceManager::destroy);
    }

    /**
     * синхронно выполняет "вызов", после чего возвращает результат выполнения
     *
     * @param call
     * @return
     */
    private AccountBalanceCallResult executeCall(AccountBalanceCall call) {
        log.debug("executing the call: " + call);
        balanceCallRepo.putNewCall(call);
        var result = waitForCallResult(call);

        if (result.hasError()) {
            log.error("call executing failed! call: {}; error: {}", call, result.getErrorMessage());
            throw new AccountException(call.getAccountId(), "Account balance call failed: " + result.getErrorMessage());
        }

        log.debug("call executing done. result: " + result);
        return result;
    }

    public static final int CALL_RESULT_WAITING_TIMEOUT = 60_000;
    public static final int CALL_RESULT_CHECKING_TIMEOUT = 1000;

    private AccountBalanceCallResult waitForCallResult(AccountBalanceCall call) {
        log.debug("start waiting result for call: " + call.getId());
        /*
         * (*) можно подумать, как лучше реализовать ожидание... например, через какой то аналог корутин (Go, Kotlin) или
         * через "подписку на события", которую сделать через очередь.
         * Или попробовать асинхронную библиотеку Quasar!
         * (*) можно сделать макс. время ожидание для случаев, когда на вызов долго не обрабатывается (AccountBalanceManager сломался).
         * А также сделать отдельный сервис, который проставляет результат "Ошибка" для вызовов, которые так и не были обработаны.
         */
        CountdownTimer timer = new CountdownTimer(CALL_RESULT_WAITING_TIMEOUT);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(CALL_RESULT_CHECKING_TIMEOUT);
            } catch (InterruptedException e) {
                break;
            }

            log.debug("checking result for call: " + call.getId());
            AccountBalanceCallResult result = balanceCallRepo.getCallResult(call.getId());
            if (result != null)
                return result;

            if (timer.isTimeOver()) {
                throw new AccountException(
                        call.getAccountId(),
                        "Call result not received in an appropriate time: " + call);
            }
        }

        throw new AccountException(call.getAccountId(), "Waiting for call result was interrupted: " + call);
    }

    //endregion
}
