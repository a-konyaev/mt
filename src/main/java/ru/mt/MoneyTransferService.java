package ru.mt;

import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor
public class MoneyTransferService {
    private final AccountService accountService;

    public Set<String> getAccounts() {
        return accountService.getAccounts();
    }

    public String createNewAccount() {
        return accountService.createNewAccount();
    }

    public double getAccountBalance(String accountId) {
        return accountService.getAccountBalance(accountId);
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
     * @return ИД транзакции (timeUUID!), которая была создана для перевода денег
     */
    public String transferMoneyAsync(String accountIdFrom, String accountIdTo, double amount) {
        /*
        - публикует запрос в Очередь для TransactionProcessor-а, которая:
          - партицианируется по ИД запроса
          - разгребается "Обработчиком очереди запросов"
          - обработчиков может быть несколько, но каждый обрабатывает свой диапазон запросов,
            т.е. запрос с одним и тем же ИД придет в этот же обработчик
        - если выполнять не надо синхронно, то возвращает ИД запроса и статус = CREATED
        - иначе,
         */
        return null;
    }

    public TransactionStatus getTransactionStatus(String transactionId) {
        /*
        - найти запись: select from transfer_query where id = ИД запроса and макс. дата
        - если нашли, то вернуть ее статус
        - если не нашли, то вернуть статус CREATED
         */
        return TransactionStatus.CREATED;
    }
}
