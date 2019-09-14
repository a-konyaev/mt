package ru.mt;

import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.data.AccountRepository;
import ru.mt.domain.*;

/*
  Отвечает за управление балансом своей пачки счетов,
  т.е. только этот AccountBalanceManager имеет доступ, причем синхронный, к счетам, за который отвечает.
 */
@Log4j2
public class AccountBalanceManager extends Component {

    private final int shardIndex;
    private final AccountBalanceCallRepository balanceCallRepo;
    private final AccountRepository accountRepo;

    AccountBalanceManager(int shardIndex) {
        this.shardIndex = shardIndex;
        balanceCallRepo = Configuration.getComponent(AccountBalanceCallRepository.class);
        accountRepo = Configuration.getComponent(AccountRepository.class);

        startCallProcessing();
    }

    @Override
    protected void destroyInternal() {
        stopCallProcessing();
    }

    //region Call processing

    private Thread callProcessingThread;

    private void startCallProcessing() {
        callProcessingThread = new Thread(this::processCalls, String.format("abm-%04d", shardIndex));
        callProcessingThread.start();
    }

    private void stopCallProcessing() {
        if (callProcessingThread == null)
            return;

        callProcessingThread.interrupt();
        try {
            // wait 1 second until the thread finishes its work
            callProcessingThread.join(1000);
        } catch (InterruptedException e) {
            log.error("Error while interruption callProcessingThread", e);
        }
    }

    private void processCalls() {
        log.debug("starting work...");

        while (!Thread.currentThread().isInterrupted() && !isDestroying()) {
            log.debug("getting next call...");

            AccountBalanceCall call;
            try {
                // wait 1 second for next call
                call = balanceCallRepo.getNextCall(shardIndex, 1000);
            } catch (InterruptedException e) {
                break;
            }

            if (call != null) {
                executeCall(call);
            }
        }

        log.debug("work had interrupted");
    }

    private void executeCall(AccountBalanceCall call) {
        log.debug("executing call: " + call);
        var resultBuilder = AccountBalanceCallResult.builder().callId(call.getId());

        try {
            switch (call.getCallType()) {
                case GET_AVAILABLE_BALANCE:
                    var balance = getAvailableBalance(call.getAccountId());
                    resultBuilder.amount(balance);
                    break;

                case RESERVE_AMOUNT:
                    var status = reserveAmount(call.getAccountId(), call.getTransactionId(), call.getAmount());
                    resultBuilder.reservationStatus(status);
                    break;

                case DEBIT_RESERVED_AMOUNT:
                    debitReservedAmount(call.getAccountId(), call.getTransactionId());
                    break;

                case CANCEL_RESERVED_AMOUNT:
                    cancelReservedAmount(call.getAccountId(), call.getTransactionId());
                    break;

                case ADD_AMOUNT:
                    addAmount(call.getAccountId(), call.getTransactionId(), call.getAmount());
                    break;

                default:
                    throw new IllegalStateException("Unknown call type: " + call.getCallType());
            }

        } catch (Throwable e) {
            var msg = String.format("Call (id = %s) execution failed: %s", call.getId(), e.getMessage());
            resultBuilder.errorMessage(msg);
            log.error(msg, e);
        }

        var result = resultBuilder.build();
        log.debug("setting call result: " + result);
        balanceCallRepo.setCallResult(call.getId(), result);
    }

    //endregion

    //region Work with account balance

    /**
     * доступная сумма = текущий баланс - сумма зарезервированных средств
     *
     * @param accountId ИД счета
     * @return доступная сумма на балансе с учетом всех зарезервированных средств
     */
    private double getAvailableBalance(String accountId) {
        var balance = accountRepo.getAccount(accountId).getBalance();

        var totalReserved = accountRepo.getAllReservationWhereStatusOK(accountId)
                .stream()
                .mapToDouble(Reservation::getAmount)
                .sum();

        return balance - totalReserved;
    }

    /**
     * Зарезервировать деньги на счете
     *
     * @param accountId     ИД счета
     * @param transactionId ИД транзакции, в рамках которой выполнить резервирование
     * @param amount        сумма денег
     * @return статус резервирования
     */
    private ReservationStatus reserveAmount(String accountId, String transactionId, double amount) {
        var reservation = accountRepo.findReservation(accountId, transactionId);

        // если ранее уже резервировали, то вернем статус этого резервирования
        if (reservation != null) {
            return reservation.getStatus();
        }

        // получим сумму на счете с учетом всех ранее зарезервированных денег
        var availableBalance = getAvailableBalance(accountId);
        // если не хватает денег на счете
        if (availableBalance < amount) {
            return ReservationStatus.DENIED;
        }

        // создаем новое резервирование
        var newReservation = new Reservation(accountId, transactionId, amount);
        newReservation.setStatus(ReservationStatus.OK);
        accountRepo.saveNewReservation(newReservation);

        return newReservation.getStatus();
    }

    /**
     * списать ранее зарезервированную сумму со счета
     *
     * @param accountId
     * @param transactionId
     * @throws
     */
    private void debitReservedAmount(String accountId, String transactionId) {
        var reservation = accountRepo.getReservation(accountId, transactionId);
        ensureReservationStatusOK(reservation);

        var account = accountRepo.getAccount(accountId);
        var newBalance = account.getBalance() - reservation.getAmount();

        accountRepo.updateAccountBalanceAndReservationStatus(
                accountId, transactionId, newBalance, ReservationStatus.DEBITED);
    }

    /**
     * отменить ранее созданное резервирование суммы
     *
     * @param accountId
     * @param transactionId
     * @throws
     */
    private void cancelReservedAmount(String accountId, String transactionId) {
        var reservation = accountRepo.getReservation(accountId, transactionId);
        ensureReservationStatusOK(reservation);
        accountRepo.updateReservationStatus(accountId, transactionId, ReservationStatus.CANCELED);
    }

    private void ensureReservationStatusOK(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.OK) {
            throw new IllegalStateException("Reservation status is not OK");
        }
    }

    /**
     * добавить сумму на счет
     *
     * @param accountId
     * @param transactionId
     * @param amount
     * @return статус OK/ERROR
     */
    private void addAmount(String accountId, String transactionId, double amount) {
        /*
        - увеличиваем баланс счета в табл. account
        - (*) если реализовать функцию блокировки счета, то можно вернуть ошибку.
         */
        // если ошибка, то кидать исключение

        //todo...
    }

    //endregion
}
