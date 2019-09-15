package ru.mt.data.inmemory;

import ru.mt.app.Component;
import ru.mt.data.TransactionRepository;
import ru.mt.domain.Transaction;
import ru.mt.domain.TransactionStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class InMemoryTransactionRepository extends Component implements TransactionRepository {

    private final Map<String, TransactionTableRow> transactionTable = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();

    @Override
    public void saveNewTransaction(Transaction transaction) {
        transactionTable.put(transaction.getId(), new TransactionTableRow(transaction));
        transactionQueue.add(transaction);
    }

    @Override
    public TransactionStatus waitForNewTransactionStatus(
            String transactionId, TransactionStatus previousStatus, long timeoutMillis) throws InterruptedException {
        var row = getTransactionTableRow(transactionId);
        return row.waitForNewStatus(previousStatus, timeoutMillis);
    }

    @Override
    public Transaction getNextTransaction(long timeoutMillis) throws InterruptedException {
        //todo: BlockingQueue не умеет делать peek с ожиданием, поэтому делаем pool,
        // но т.к. он удаляет элемент из очереди, то реализация commitTransactionHasBeenProcessed
        // останется пустой, т.к. смысл комита как раз в том, чтобы удалить эл-т из очереди
        // PS: эту проблему можно будет решить при реализации репозитория через БД
        return transactionQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void commitTransactionHasBeenProcessed(String transactionId) {
        //todo: см. комментарий к getNextTransaction
    }

    @Override
    public void updateTransactionStatus(String transactionId, TransactionStatus status) {
        var row = getTransactionTableRow(transactionId);
        row.setStatus(status);
    }

    private TransactionTableRow getTransactionTableRow(String transactionId) {
        var row = transactionTable.get(transactionId);
        if (row == null) {
            throw new IllegalStateException("Transaction not found: " + transactionId);
        }

        return row;
    }
}
