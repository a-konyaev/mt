package ru.mt.domain;

import lombok.Getter;
import lombok.ToString;
import ru.mt.utils.TimeUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@ToString
public class AccountBalanceCall {
    private final String id = UUID.randomUUID().toString();
    private final long ts = TimeUtils.getTimestamp();

    private final AccountBalanceCallType callType;
    private final String accountId;
    private final String transactionId;
    private final Double amount;

    //region constructors

    private AccountBalanceCall(
            AccountBalanceCallType callType,
            String accountId,
            String transactionId,
            Double amount) {
        this.callType = callType;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.amount = amount;
    }

    public static AccountBalanceCall getAvailableBalance(String accountId) {
        return new AccountBalanceCall(
                AccountBalanceCallType.GET_AVAILABLE_BALANCE,
                accountId,
                null,
                null);
    }

    public static AccountBalanceCall reserveAmount(String accountId, String transactionId, double amount) {
        return new AccountBalanceCall(
                AccountBalanceCallType.RESERVE_AMOUNT,
                accountId,
                transactionId,
                amount);
    }

    public static AccountBalanceCall debitReservedAmount(String accountId, String transactionId) {
        return new AccountBalanceCall(
                AccountBalanceCallType.DEBIT_RESERVED_AMOUNT,
                accountId,
                transactionId,
                null);
    }

    public static AccountBalanceCall cancelReservedAmount(String accountId, String transactionId) {
        return new AccountBalanceCall(
                AccountBalanceCallType.CANCEL_RESERVED_AMOUNT,
                accountId,
                transactionId,
                null);
    }

    public static AccountBalanceCall addAmount(String accountId, String transactionId, double amount) {
        return new AccountBalanceCall(
                AccountBalanceCallType.ADD_AMOUNT,
                accountId,
                transactionId,
                amount);
    }

    //endregion
}
