package ru.mt.data.inmemory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.mt.domain.Transaction;
import ru.mt.domain.TransactionStatus;

@RequiredArgsConstructor
class TransactionTableRow {
    private final Object signal = new Object();
    @Getter
    private final Transaction transaction;
    /**
     * Храним статус отдельно, чтобы контролировать его изменения
     */
    @Getter
    private volatile TransactionStatus status = TransactionStatus.CREATED;

    void setStatus(TransactionStatus status) {
        synchronized (signal) {
            this.status = status;
            signal.notifyAll();
        }
    }

    TransactionStatus waitForNewStatus(TransactionStatus previousStatus, long timeoutMillis) throws InterruptedException {
        if (previousStatus.isFinal()) {
            throw new IllegalArgumentException("The previous status does not have to be final");
        }

        synchronized (signal) {
            // if status already changed
            if (!status.equals(previousStatus)) {
                return status;
            }

            signal.wait(timeoutMillis);
            return status;
        }
    }
}
