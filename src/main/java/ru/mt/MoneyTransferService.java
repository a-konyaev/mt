package ru.mt;

import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.data.TransactionRepository;
import ru.mt.domain.Transaction;
import ru.mt.domain.TransactionStatus;
import ru.mt.errors.MoneyTransferException;
import ru.mt.errors.MoneyTransferTransactionException;
import ru.mt.errors.MoneyTransferValidationException;
import ru.mt.utils.CountdownTimer;
import ru.mt.utils.Processor;

import java.math.BigDecimal;
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

        initCashDesk();
        startTransactionProcessing();
    }

    @Override
    protected void destroyInternal() {
        stopTransactionProcessing();
    }

    //region cash desk

    /**
     * Условная сумма денег, которую могут внести в кассу, т.е. сумма денег вне системы (1 квадриллион)
     */
    private static final BigDecimal CASH_DESK_INIT_BALANCE = new BigDecimal(1_000_000_000_000_000L);
    /**
     * Технический счет для денег, которые приняты в кассе для зачисления на счет
     */
    private String cashDeskInAccountId;
    /**
     * Технический счет для денег, которые выданы в кассе со счета
     */
    private String cashDeskOutAccountId;
    // todo: сделать API для получения баланса тех. счетов.

    private void initCashDesk() {
        cashDeskInAccountId = accountService.createNewAccount();
        cashDeskOutAccountId = accountService.createNewAccount();

        // счет для денег, которые принимаем в кассе, устанавливаем в макс. значение, т.е.
        // не ограничиваем кол-во денег "вне системы"
        var result = accountService.addAmount(cashDeskInAccountId, null, CASH_DESK_INIT_BALANCE);
        if (result.hasError()) {
            throw new IllegalStateException("Cash desk accounts initialization error: " + result.getErrorMessage());
        }
    }

    //endregion

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

    public BigDecimal getAccountBalance(String accountId) throws MoneyTransferException {
        validateAccount(accountId);

        var result = accountService.getAccountBalance(accountId);
        if (result.hasError()) {
            throw new MoneyTransferException("Getting account balance error: " + result.getErrorMessage());
        }

        return result.getAmount();
    }

    public void putMoneyIntoAccount(String accountId, BigDecimal amount) throws MoneyTransferException {
        validateAccount(accountId);
        validateAmount(amount);

        var transactionId = registerNewTransaction(cashDeskInAccountId, accountId, amount);
        waitTransactionCompleted(transactionId);
    }

    public void withdrawMoneyFromAccount(String accountId, BigDecimal amount) throws MoneyTransferException {
        validateAccount(accountId);
        validateAmount(amount);

        var transactionId = registerNewTransaction(accountId, cashDeskOutAccountId, amount);
        waitTransactionCompleted(transactionId);
    }

    public void transferMoney(String accountIdFrom, String accountIdTo, BigDecimal amount) throws MoneyTransferException {
        validateAccount(accountIdFrom);
        validateAccount(accountIdTo);
        if (accountIdFrom.equals(accountIdTo)) {
            throw new MoneyTransferValidationException("From and To accounts must be different");
        }
        validateAmount(amount);

        var transactionId = registerNewTransaction(accountIdFrom, accountIdTo, amount);
        waitTransactionCompleted(transactionId);
    }

    //region validation

    /**
     * Максимальная сумма денег для обработки в одной транзакции (1 млрд)
     */
    static final BigDecimal ONE_TRANSACTION_MAX_AMOUNT = new BigDecimal(1_000_000_000);
    /**
     * Максимальное кол-во знаков после запятой для сумм.
     * Значение 2 означает, что разрешены суммы вида N, N.M, N.MM, и запрещены вида N.MM...M, где кол-во M > 2
     */
    private static final int AMOUNT_MAX_SCALE = 2;


    private static void validateAccount(String accountId) throws MoneyTransferValidationException {
        if (accountId == null || accountId.isEmpty()) {
            throw new MoneyTransferValidationException("The account id must not be empty");
        }
    }

    private static void validateAmount(BigDecimal amount) throws MoneyTransferValidationException {
        if (amount == null) {
            throw new MoneyTransferValidationException("The amount must be set");
        }

        if (BigDecimal.ZERO.compareTo(amount) >= 0) {
            throw new MoneyTransferValidationException("The amount must be positive; amount: " + amount);
        }

        if (ONE_TRANSACTION_MAX_AMOUNT.compareTo(amount) < 0) {
            throw new MoneyTransferValidationException(String.format(
                    "The amount must be less then or equal to %s; amount: %s", ONE_TRANSACTION_MAX_AMOUNT, amount));
        }

        if (amount.scale() > AMOUNT_MAX_SCALE) {
            throw new MoneyTransferValidationException(String.format(
                    "The amount scale must be less then or equal to %d; scale: %d", AMOUNT_MAX_SCALE, amount.scale()));
        }
    }

    //endregion

    //endregion

    //region Transaction registration and waiting completion

    /**
     * Max waiting transaction final status timeout (60 sec)
     */
    private static final int TRANSACTION_COMPLETE_TIMEOUT = 60_000;

    private String registerNewTransaction(String accountIdFrom, String accountIdTo, BigDecimal amount) {
        var transaction = new Transaction(accountIdFrom, accountIdTo, amount, TransactionStatus.CREATED);
        transactionRepo.saveNewTransaction(transaction);
        return transaction.getId();
    }

    private void waitTransactionCompleted(String transactionId) throws MoneyTransferException {
        var status = waitTransactionFinalStatus(transactionId);
        switch (status) {
            case DONE:
                return; // it's alright

            case ERROR:
            case DENIED:
                throw new MoneyTransferTransactionException(transactionId, status);

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
                return TransactionStatus.ERROR.setReason("Transaction not completed in an appropriate time");
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
                    currentStatus = processCANCELLING(transaction, currentStatus.getReason());
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
            return TransactionStatus.ERROR.setReason(result.getErrorMessage());
        }

        var reservationStatus = result.getReservationStatus();
        switch (reservationStatus) {
            case OK:
                return TransactionStatus.RESERVED;

            case DENIED:
            case CANCELED:
                return TransactionStatus.DENIED.setReason(reservationStatus.getReason());

            case DEBITED:
                // какой то сбой: деньги по транзакции уже списаны со счета account_from,
                // но статус транзакции такой, что мы еще не добрались до увеличения счета account_to
                return TransactionStatus.ERROR
                        .setReason("Unexpected reservation status 'DEBITED' for account 'From' " +
                                "before account 'To' will be credited");

            default:
                throw new IllegalStateException("Unexpected reservation status: " + reservationStatus);
        }
    }

    private TransactionStatus processRESERVED(Transaction transaction) {
        var result = accountService.addAmount(
                transaction.getAccountIdTo(), transaction.getId(), transaction.getAmount());

        if (result.hasError()) {
            // не смогли добавить деньги на счет получателя
            // => нужно отменить резервирование денег на счете отправителя
            return TransactionStatus.CANCELLING.setReason(result.getErrorMessage());
        }

        return TransactionStatus.ADDED;
    }

    private TransactionStatus processADDED(Transaction transaction) {
        var result = accountService.debitReservedAmount(transaction.getAccountIdFrom(), transaction.getId());

        if (result.hasError()) {
            // не смогли списать ранее зарезервированные деньги
            return TransactionStatus.ERROR.setReason(result.getErrorMessage());
        }

        return TransactionStatus.DONE;
    }

    private TransactionStatus processCANCELLING(Transaction transaction, String reason) {
        var result = accountService.cancelReservedAmount(transaction.getAccountIdFrom(), transaction.getId());

        if (result.hasError()) {
            // не смогли отменить ранее зарезервированные деньги
            return TransactionStatus.ERROR.setReason(
                    String.format("Error while cancelling transaction for reason '%s': %s",
                            reason, result.getErrorMessage()));
        }

        return TransactionStatus.DENIED.setReason(reason);
    }

    //endregion
}
