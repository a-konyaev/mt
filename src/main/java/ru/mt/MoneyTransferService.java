package ru.mt;

import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.data.TransactionRepository;
import ru.mt.domain.AccountBalanceCallResult;
import ru.mt.domain.Transaction;
import ru.mt.domain.TransactionStatus;
import ru.mt.utils.Assert;
import ru.mt.utils.CountdownTimer;
import ru.mt.utils.Processor;

import java.util.Set;

/**
 * Обработчик транзакций по переводу денег.
 */
@Log4j2
public class MoneyTransferService extends Component {
    private final AccountService accountService;
    private final TransactionRepository transactionRepo;

    public MoneyTransferService() {
        accountService = Configuration.getComponent(AccountService.class);
        transactionRepo = Configuration.getComponent(TransactionRepository.class);

        initCashDeskAccounts();
        startTransactionProcessing();
    }

    @Override
    protected void destroyInternal() {
        stopTransactionProcessing();
    }

    /**
     * Технический счет для денег, которые приняты в кассе для зачисления на счет
     */
    private String cashDeskInAccountId;
    /**
     * Технический счет для денег, которые выданы в кассе со счета
     */
    private String cashDeskOutAccountId;

    private void initCashDeskAccounts() {
        cashDeskInAccountId = accountService.createNewAccount();
        cashDeskOutAccountId = accountService.createNewAccount();

        // счет для денег, которые принимаем в кассе, устанавливаем в макс. значение, т.е.
        // не ограничиваем кол-во денег "вне системы"
        var result = accountService.addAmount(cashDeskInAccountId, null, Double.MAX_VALUE);
        if (result.hasError()) {
            throw new IllegalStateException("Cash desk accounts initialization error: " + result.getErrorMessage());
        }
    }

    //region public API

    public Set<String> getAccounts() {
        var accounts = accountService.getAccounts();

        // удалим технические счета
        accounts.remove(cashDeskInAccountId);
        accounts.remove(cashDeskOutAccountId);

        return accounts;
    }

    public String createNewAccount() {
        return accountService.createNewAccount();
    }

    public double getAccountBalance(String accountId) {
        assertAccountNotEmpty(accountId);

        var result = accountService.getAccountBalance(accountId);
        if (result.hasError()) {
            throw new IllegalStateException("Getting account balance error: " + result.getErrorMessage());
        }

        return result.getAmount();
    }

    public void putMoneyIntoAccount(String accountId, double amount) {
        assertAccountNotEmpty(accountId);
        assertAmountPositive(amount);

        var transactionId = registerNewTransaction(cashDeskInAccountId, accountId, amount);
        waitTransactionCompleted(transactionId);
    }

    public void withdrawMoneyFromAccount(String accountId, double amount) {
        assertAccountNotEmpty(accountId);
        assertAmountPositive(amount);

        var transactionId = registerNewTransaction(accountId, cashDeskOutAccountId, amount);
        waitTransactionCompleted(transactionId);
    }

    public void transferMoney(String accountIdFrom, String accountIdTo, double amount) {
        assertAccountNotEmpty(accountIdFrom);
        assertAccountNotEmpty(accountIdTo);
        if (accountIdFrom.equals(accountIdTo)) {
            throw new IllegalArgumentException("From and To accounts must be different");
        }
        assertAmountPositive(amount);

        var transactionId = registerNewTransaction(accountIdFrom, accountIdTo, amount);
        waitTransactionCompleted(transactionId);
    }

    private static void assertAccountNotEmpty(String accountId) {
        Assert.notEmpty(accountId, "Account id must not be empty");
    }

    private static void assertAmountPositive(double amount) {
        Assert.positive(amount, "Amount must be positive! amount: " + amount);
    }

    //endregion

    //region Transaction registration and waiting completion

    /**
     * Max waiting transaction final status timeout (60 sec)
     */
    private static final int TRANSACTION_COMPLETE_TIMEOUT = 60_000;

    private String registerNewTransaction(String accountIdFrom, String accountIdTo, double amount) {
        var transaction = new Transaction(accountIdFrom, accountIdTo, amount, TransactionStatus.CREATED);
        transactionRepo.saveNewTransaction(transaction);
        return transaction.getId();
    }

    private void waitTransactionCompleted(String transactionId) {
        var status = waitTransactionFinalStatus(transactionId);
        switch (status) {
            case DONE:
                return; // it's alright

            case ERROR:
                throw new RuntimeException("Transaction processing failed");

            case DENIED:
                throw new RuntimeException("Transaction processing denied");

            default:
                throw new IllegalStateException("Unexpected transaction status: " + status);
        }
    }

    private TransactionStatus waitTransactionFinalStatus(String transactionId) {
        log.debug("start waiting for the transaction final status: " + transactionId);

        CountdownTimer timer = new CountdownTimer(TRANSACTION_COMPLETE_TIMEOUT);
        var currentStatus = TransactionStatus.CREATED;

        while (!Thread.currentThread().isInterrupted() && !isDestroying()) {
            log.debug("checking transaction status: " + transactionId);

            try {
                var newStatus = transactionRepo.waitForNewTransactionStatus(
                        transactionId, currentStatus, 1000);
                log.debug("Transaction (id={}) status changed: {}", transactionId, newStatus);

                if (newStatus.isFinal()) {
                    return newStatus;
                }

                currentStatus = newStatus;
                // go to the next iteration
            } catch (InterruptedException e) {
                break;
            }

            if (timer.isTimeOver()) {
                throw new RuntimeException("Transaction not completed in an appropriate time: " + transactionId);
            }
        }

        throw new RuntimeException("Waiting for transaction final status was interrupted: " + transactionId);
    }

    //endregion

    //region Transaction processing

    //todo: пока один процессор, но можно сделать несколько, чтобы каждый обрабатывал свою партицию транзакций
    private Processor transactionProcessor;

    private void startTransactionProcessing() {
        transactionProcessor = new Processor("transactions", this::processTransactions);
        transactionProcessor.start();
    }

    private void stopTransactionProcessing() {
        transactionProcessor.stop();
    }

    private void processTransactions() throws InterruptedException {
        log.debug("getting next transaction...");
        var transaction = transactionRepo.getNextTransaction(1000);
        if (transaction == null) {
            return;
        }

        log.debug("process transaction: " + transaction);
        processTransaction(transaction);

        transactionRepo.commitTransactionHasBeenProcessed(transaction.getId());
    }

    private void processTransaction(Transaction transaction) {
        var currentStatus = transaction.getStatus();

        while (true) {
            switch (currentStatus) {
                case CREATED: // новая транзакция
                    currentStatus = processCREATED(transaction);
                    break;

                case RESERVED: // деньги зарезервированы у счета From
                    currentStatus = processRESERVED(transaction);
                    break;

                case ADDED:
                    currentStatus = processADDED(transaction);
                    break;

                case CANCELLING:
                    currentStatus = processCANCELLING(transaction);
                    break;

                case DONE:
                case DENIED:
                case ERROR:
                    return;

                default:
                    throw new IllegalStateException("Unexpected transaction status: " + currentStatus);
            }

            transactionRepo.updateTransactionStatus(transaction.getId(), currentStatus);
        }
    }

    private TransactionStatus processCREATED(Transaction transaction) {
        var result = accountService.reserveAmount(
                transaction.getAccountIdFrom(), transaction.getId(), transaction.getAmount());

        if (result.hasError()) {
            return TransactionStatus.ERROR;
        }

        var reservationStatus = result.getReservationStatus();
        switch (reservationStatus) {
            case OK:
                return TransactionStatus.RESERVED;

            case DENIED:
            case CANCELED:
                return TransactionStatus.DENIED;

            case DEBITED:
                // какой то сбой: деньги по транзакции уже списаны со счета account_from,
                // но статус транзакции такой, что мы еще не добрались до увеличения счета account_to
                return TransactionStatus.ERROR;

            default:
                throw new IllegalStateException("Unexpected reservation status: " + reservationStatus);
        }
    }

    private TransactionStatus processRESERVED(Transaction transaction) {
        var result = accountService.addAmount(
                transaction.getAccountIdTo(), transaction.getId(), transaction.getAmount());

        if (result.hasError()) {
            // не смогли добавить деньги на счет получателя => нужно отменить резервирование денег на счете отправителя
            return TransactionStatus.CANCELLING;
        }

        return TransactionStatus.ADDED;
    }

    private TransactionStatus processADDED(Transaction transaction) {
        var result = accountService.debitReservedAmount(transaction.getAccountIdFrom(), transaction.getId());

        if (result.hasError()) {
            // не смогли списать ранее зарезервированные деньги
            return TransactionStatus.ERROR;
        }

        return TransactionStatus.DONE;
    }

    private TransactionStatus processCANCELLING(Transaction transaction) {
        var result = accountService.cancelReservedAmount(transaction.getAccountIdFrom(), transaction.getId());

        if (result.hasError()) {
            // не смогли отменить ранее зарезервированные деньги
            return TransactionStatus.ERROR;
        }

        return TransactionStatus.DENIED;
    }

    //endregion
}
