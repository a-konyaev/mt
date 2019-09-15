package ru.mt.data.inmemory;

import lombok.RequiredArgsConstructor;
import ru.mt.app.Component;
import ru.mt.data.AccountRepository;
import ru.mt.domain.Account;
import ru.mt.domain.Reservation;
import ru.mt.domain.ReservationStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryAccountRepository extends Component implements AccountRepository {

    private Map<String, AccountTableRow> accountTable = new ConcurrentHashMap<>();

    @RequiredArgsConstructor
    private static class AccountTableRow {
        final Account account;
        /**
         * Key: transaction id
         * Value: reservation
         */
        final Map<String, Reservation> reservations = new HashMap<>();
    }

    private AccountTableRow getAccountTableRow(String accountId) {
        var row = accountTable.get(accountId);
        if (row == null) {
            throw new IllegalStateException("Account not found: " + accountId);
        }

        return row;
    }

    //region accounts

    @Override
    public void saveNewAccount(Account account) {
        var id = account.getId();
        if (accountTable.containsKey(id)) {
            throw new IllegalStateException("Account with the same id already exists: " + id);
        }

        var row = new AccountTableRow(account);
        accountTable.put(id, row);
    }

    @Override
    public Set<String> findAllAccount() {
        // makes a copy to prevent reflection of the accounts map's changes in this set of keys
        return new HashSet<>(accountTable.keySet());
    }

    @Override
    public Account findAccount(String accountId) {
        var row = accountTable.get(accountId);
        return row != null ? row.account : null;
    }

    //endregion

    //region reservations

    @Override
    public void saveNewReservation(Reservation reservation) {
        var row = getAccountTableRow(reservation.getAccountId());

        var transactionId = reservation.getTransactionId();
        if (row.reservations.containsKey(transactionId)) {
            throw new IllegalStateException(
                    "Reservation with the same transaction id already exists: " + transactionId);
        }

        row.reservations.put(transactionId, reservation);
    }

    @Override
    public Reservation findReservation(String accountId, String transactionId) {
        // todo: для оптимизации поиска можно использовать timeUUID в качестве transactionId
        //  и выполнять поиск транзакций, которые не старше, чем макс. время
        var row = getAccountTableRow(accountId);
        return row.reservations.get(transactionId);
    }

    @Override
    public Set<Reservation> getAllReservationWhereStatusOK(String accountId) {
        var row = getAccountTableRow(accountId);
        return row.reservations.values()
                .stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.OK)
                .collect(Collectors.toSet());
    }

    /**
     * смысл в одном методе - сделать все изменения в одной общей транзакции
     */
    @Override
    public void updateAccountBalanceAndReservationStatus(
            String accountId, String transactionId, double balance, ReservationStatus status) {
        var row = getAccountTableRow(accountId);
        var reservation = getReservation(row, transactionId);
        reservation.setStatus(status);
        row.account.setBalance(balance);
    }

    @Override
    public void updateReservationStatus(String accountId, String transactionId, ReservationStatus status) {
        var row = getAccountTableRow(accountId);
        var reservation = getReservation(row, transactionId);
        reservation.setStatus(status);
    }

    @Override
    public void updateAccountBalance(String accountId, double balance) {
        var row = getAccountTableRow(accountId);
        row.account.setBalance(balance);
    }

    private Reservation getReservation(AccountTableRow row, String transactionId) {
        var reservation = row.reservations.get(transactionId);
        if (reservation == null) {
            throw new IllegalStateException("Reservation not found: " + transactionId);
        }

        return reservation;
    }

    //endregion
}
