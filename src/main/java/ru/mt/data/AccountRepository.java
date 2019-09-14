package ru.mt.data;

import ru.mt.domain.Account;
import ru.mt.domain.Reservation;
import ru.mt.domain.ReservationStatus;

import java.util.Set;

/**
 * Repository for Account entities
 */
public interface AccountRepository {

    void saveNewAccount(Account account);

    Set<String> findAllAccount();

    Account findAccount(String accountId);

    Account getAccount(String accountId);

    void saveNewReservation(Reservation reservation);

    Reservation findReservation(String accountId, String transactionId);

    Reservation getReservation(String accountId, String transactionId);

    Set<Reservation> getAllReservationWhereStatusOK(String accountId);

    void updateAccountBalanceAndReservationStatus(
            String accountId, String transactionId, double balance, ReservationStatus status);

    void updateReservationStatus(String accountId, String transactionId, ReservationStatus status);
}
