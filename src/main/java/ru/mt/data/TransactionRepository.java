package ru.mt.data;

import ru.mt.domain.Transaction;
import ru.mt.domain.TransactionStatus;

public interface TransactionRepository {

    void saveNewTransaction(Transaction transaction);

    TransactionStatus waitForNewTransactionStatus(
            String transactionId, TransactionStatus previousStatus, long timeoutMillis) throws InterruptedException;

    Transaction getNextTransaction(long timeoutMillis) throws InterruptedException;

    void commitTransactionHasBeenProcessed(String transactionId);

    void updateTransactionStatus(String transactionId, TransactionStatus status);
}
