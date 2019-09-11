package ru.mt;

import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor
public class MoneyTransferService {
    private final AccountService accountService;
    private final TransactionProcessor transactionProcessor;


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
        // ждем завершения транзакциии...
        waitTransactionCompleted(transactionId);
    }

    public void withdrawMoneyFromAccount(String accountId, double amount) {
        var transactionId = transactionProcessor.registerwithdrawMoneyTransaction(accountId, amount);
        // ждем завершения транзакциии...
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
        /*
        - публикует новый запрос в Очередь для TransactionProcessor-а, которая:
          - партицианируется по ИД запроса
          - обработчиков (TransactionProcessor-ов) может быть несколько, но каждый обрабатывает свой диапазон запросов,
            т.е. запрос с одним и тем же ИД придет в этот же обработчик
         */
        var transactionId = transactionProcessor.registerTransferMoneyTransaction(accountIdFrom, accountIdTo, amount);
        /*
        периодически выполняет:
          - проверить статус запроса
          - если он финальный (Готово или Ошибка), то вернуть статус
          - иначе, ждет таймаут и на новую итерацию
         */
        waitTransactionCompleted(transactionId);
    }

    private void waitTransactionCompleted(String transactionId) {
        var transactionStatus = transactionProcessor.getTransactionStatus(transactionId);
        //todo: если не смогли, то поднять исключение
        // добавить отдельный тип исключений для этого сервиса
    }
}
