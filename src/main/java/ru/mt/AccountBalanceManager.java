package ru.mt;

import lombok.extern.log4j.Log4j2;
import ru.mt.app.Component;
import ru.mt.app.Configuration;
import ru.mt.data.AccountBalanceCallRepository;
import ru.mt.data.AccountRepository;
import ru.mt.domain.*;
import ru.mt.utils.Processor;

import java.math.BigDecimal;

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

    private Processor callProcessor;

    private void startCallProcessing() {
        callProcessor = new Processor(String.format("abm-%04d", shardIndex), this::processCall);
        callProcessor.start();
    }

    private void stopCallProcessing() {
        callProcessor.stop();
    }

    private void processCall() throws InterruptedException {
        log.debug("getting next call...");

        var call = balanceCallRepo.getNextCall(shardIndex, 1000);
        if (call == null) {
            return;
        }

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
            var msg = String.format("Call '%s' execution failed: %s", call.getId(), e.getMessage());
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
    private BigDecimal getAvailableBalance(String accountId) {
        var balance = getAccountBalance(accountId);

        var totalReserved = accountRepo.getAllReservationWhereStatusOK(accountId)
                .stream()
                .map(Reservation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return balance.subtract(totalReserved);
    }

    private BigDecimal getAccountBalance(String accountId) {
        var account = accountRepo.findAccount(accountId);
        if (account == null) {
            throw new IllegalStateException("Account not found: " + accountId);
        }

        return account.getBalance();
    }

    /**
     * Зарезервировать деньги на счете
     *
     * @param accountId     ИД счета
     * @param transactionId ИД транзакции, в рамках которой выполнить резервирование
     * @param amount        сумма денег
     * @return статус резервирования
     */
    private ReservationStatus reserveAmount(String accountId, String transactionId, BigDecimal amount) {
        var reservation = accountRepo.findReservation(accountId, transactionId);

        // если ранее уже резервировали, то вернем статус этого резервирования
        if (reservation != null) {
            return reservation.getStatus();
        }

        // получим сумму на счете с учетом всех ранее зарезервированных денег
        var availableBalance = getAvailableBalance(accountId);
        // если не хватает денег на счете
        if (availableBalance.compareTo(amount) < 0) {
            return ReservationStatus.DENIED.setReason(
                    String.format("Available balance %s below required %s", availableBalance, amount));
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
     */
    private void debitReservedAmount(String accountId, String transactionId) {
        var reservation = getReservationCheckStatusOK(accountId, transactionId);
        var balance = getAccountBalance(accountId);

        var newBalance = balance.subtract(reservation.getAmount());

        accountRepo.updateAccountBalanceAndReservationStatus(
                accountId, transactionId, newBalance, ReservationStatus.DEBITED);
    }

    /**
     * отменить ранее созданное резервирование суммы
     *
     * @param accountId
     * @param transactionId
     */
    private void cancelReservedAmount(String accountId, String transactionId) {
        getReservationCheckStatusOK(accountId, transactionId);
        accountRepo.updateReservationStatus(accountId, transactionId, ReservationStatus.CANCELED);
    }

    private Reservation getReservationCheckStatusOK(String accountId, String transactionId) {
        var reservation = accountRepo.findReservation(accountId, transactionId);

        if (reservation == null) {
            throw new IllegalStateException(String.format(
                    "Reservation not found (account id: %s, transaction id: %s)",
                    accountId, transactionId));
        }

        if (reservation.getStatus() != ReservationStatus.OK) {
            throw new IllegalStateException(String.format(
                    "Reservation status is not OK (account id: %s, transaction id: %s, status: %s)",
                    accountId, transactionId, reservation.getStatus()));
        }

        return reservation;
    }

    /**
     * добавить сумму на счет
     *
     * @param accountId
     * @param transactionId
     * @param amount
     * @return статус OK/ERROR
     */
    private void addAmount(String accountId, String transactionId, BigDecimal amount) {
        // todo: transactionId не используется, но в будущем можно и пополнение счета фиксировать
        //  в отдельной таблице с историей изменения баланса счета

        // todo: если реализовать функцию блокировки счета, то можно вернуть ошибку.

        var balance = getAccountBalance(accountId);
        var newBalance = balance.add(amount);
        accountRepo.updateAccountBalance(accountId, newBalance);
    }

    //endregion
}
