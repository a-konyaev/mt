package ru.mt;

import ru.mt.app.Component;
import ru.mt.app.Configuration;

import java.util.Set;

public class MoneyTransferService
        extends Component {

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

    public double getAccountBalance(String accountId) {
        return accountService.getAccountBalance(accountId);
    }

    public void putMoneyIntoAccount(String accountId, double amount) {
        var transactionId = transactionProcessor.registerPutMoneyTransaction(accountId, amount);
        waitTransactionCompleted(transactionId);
    }

    public void withdrawMoneyFromAccount(String accountId, double amount) {
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
    public void transferMoney(String accountIdFrom, String accountIdTo, double amount) {
        var transactionId = transactionProcessor.registerTransferMoneyTransaction(accountIdFrom, accountIdTo, amount);
        waitTransactionCompleted(transactionId);
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
