package ru.mt;

import lombok.Getter;

@Getter
public class AccountBalanceCall {
    private static final String EMPTY_TRANSACTION_ID = "";

    //todo: добавить уникальный ИД запроса, чтобы например, его же прописывать в ответ?

    private final AccountBalanceCallType callType;
    private final String accountId;
    private final String transactionId;
    private final double amount;

    private AccountBalanceCall(
            AccountBalanceCallType callType,
            String accountId,
            String transactionId,
            double amount) {
        this.callType = callType;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.amount = amount;
    }

    public static AccountBalanceCall getBalance(String accountId) {
        return new AccountBalanceCall(
                AccountBalanceCallType.GET_BALANCE,
                accountId,
                EMPTY_TRANSACTION_ID,
                0);
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
                0);
    }

    public static AccountBalanceCall cancelReservedAmount(String accountId, String transactionId) {
        return new AccountBalanceCall(
                AccountBalanceCallType.CANCEL_RESERVED_AMOUNT,
                accountId,
                transactionId,
                0);
    }

    public static AccountBalanceCall addAmount(String accountId, String transactionId, double amount) {
        return new AccountBalanceCall(
                AccountBalanceCallType.ADD_AMOUNT,
                accountId,
                transactionId,
                amount);
    }
}
