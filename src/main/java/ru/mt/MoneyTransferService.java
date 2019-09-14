package ru.mt;

import lombok.NonNull;
import ru.mt.app.Component;
import ru.mt.app.Configuration;

import java.util.Set;

public class MoneyTransferService extends Component {

    private final AccountService accountService;
    private final TransactionProcessor transactionProcessor;

    public MoneyTransferService() {
        accountService = Configuration.getComponent(AccountService.class);
        transactionProcessor = Configuration.getComponent(TransactionProcessor.class);
    }

    public Set<String> getAccounts() {
        return accountService.getAccounts();
    }

    public String createNewAccount() {
        return accountService.createNewAccount();
    }

    public double getAccountBalance(@NonNull String accountId) {
        return accountService.getAccountBalance(accountId);
    }

    public void putMoneyIntoAccount(@NonNull String accountId, double amount) {
        assertAmountPositive(amount);

        var transactionId = transactionProcessor.registerPutMoneyTransaction(accountId, amount);
        waitTransactionCompleted(transactionId);
    }

    public void withdrawMoneyFromAccount(@NonNull String accountId, double amount) {
        assertAmountPositive(amount);

        var transactionId = transactionProcessor.registerWithdrawMoneyTransaction(accountId, amount);
        waitTransactionCompleted(transactionId);
    }

    /**
     * перевести деньги с одного счета на другой.
     *
     * @param accountIdFrom ИД счета откуда
     * @param accountIdTo   ИД счета куда
     * @param amount        сумма
     */
    public void transferMoney(@NonNull String accountIdFrom, @NonNull String accountIdTo, double amount) {
        assertAmountPositive(amount);

        var transactionId = transactionProcessor.registerTransferMoneyTransaction(accountIdFrom, accountIdTo, amount);
        waitTransactionCompleted(transactionId);
    }

    private static void assertAmountPositive(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be positive! amount: " + amount);
    }

    private void waitTransactionCompleted(String transactionId) {
                /*
        периодически выполняет:
          - проверить статус запроса
          - если он финальный (Готово или Ошибка), то вернуть статус
          - иначе, ждет таймаут и на новую итерацию
         */
        var transactionStatus = transactionProcessor.getTransactionStatus(transactionId);
        //todo: если не смогли, то поднять исключение
        // добавить отдельный тип исключений для этого сервиса
    }
}
