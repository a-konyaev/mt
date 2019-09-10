package ru.mt;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.NonNull;

import java.util.Set;

@AllArgsConstructor
public class MoneyTransferService {
    private final AccountService accountService;
    private final TransactionProcessor transactionProcessor;


    public Set<String> getAccounts() {
        return accountService.getAccounts();
    }

    public String createNewAccount() {
        return accountService.createNewAccount().getId();
    }

    public double getAccountBalance(String accountId) {
        return accountService.getAccountBalance(accountId);
    }

    public void putMoneyIntoAccount(String accountId, double amount) {
        accountService.creditTheAccount(accountId, amount);
    }

    public void withdrawMoneyFromAccount(String accountId, double amount) {
        accountService.debitTheAccount(accountId, amount);
    }

    /**
     * перевести деньги с одного счета на другой.
     *
     * @param accountIdFrom ИД счета откуда
     * @param accountIdTo   ИД счета куда
     * @param amount        сумма
     * @return результат выполнения - успешно или ошибка
     */
    public boolean transferMoney(String accountIdFrom, String accountIdTo, double amount) {
        // еще можно добавить параметр "ИД запроса" - для клиентов, который "заикаются" из-за плохого канала
        var transactionId = transferMoneyAsync(accountIdFrom, accountIdTo, amount);
        /*
        периодически выполняет:
          - проверить статус запроса
          - если он финальный (Готово или Ошибка), то вернуть статус
          - иначе, ждет таймаут и на новую итерацию
         */
        return true;
    }

    /**
     * перевести деньги с одного счета на другой асинхронно
     *
     * @param accountIdFrom
     * @param accountIdTo
     * @param amount
     * @return ИД транзакции, которая была создана для перевода денег
     */
    public String transferMoneyAsync(String accountIdFrom, String accountIdTo, double amount) {
        /*
        - публикует новый запрос в Очередь для TransactionProcessor-а, которая:
          - партицианируется по ИД запроса
          - обработчиков (TransactionProcessor-ов) может быть несколько, но каждый обрабатывает свой диапазон запросов,
            т.е. запрос с одним и тем же ИД придет в этот же обработчик
         */
        return transactionProcessor.registerNewTransaction(accountIdFrom, accountIdTo, amount);
    }

    public TransactionStatus getTransactionStatus(String transactionId) {
        return transactionProcessor.getTransactionStatus(transactionId);
    }
}
