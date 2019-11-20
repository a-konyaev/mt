package ru.mt.errors;

import lombok.Getter;
import ru.mt.domain.TransactionStatus;

@Getter
public class MoneyTransferTransactionException extends MoneyTransferException {
    private final String transactionId;
    private final TransactionStatus transactionStatus;

    public MoneyTransferTransactionException(String transactionId, TransactionStatus transactionStatus) {
        super();
        this.transactionId = transactionId;
        this.transactionStatus = transactionStatus;
    }

    @Override
    public String toString() {
        return String.format("Transaction '%s' failed; status: %s; reason: %s",
                transactionId, transactionStatus, transactionStatus.getReason());
    }
}
