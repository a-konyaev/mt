package ru.mt;

import lombok.NonNull;
import ru.mt.app.Configuration;
import ru.mt.data.AccountRepository;
import ru.mt.domain.Account;

import java.util.Set;

/**
 * Сервис, через который выполняются все операции со счетами.
 * <p>
 * Запросы по созданию счетов выполняет сам.
 * <p>
 * Запросы по работе с балансом счета проксирует к AccountBalanceManager-ам,
 * но при этом реализует логику синхронного вызова AccountBalanceManager-ов,
 * которые работают асинхронно через обработку вызовов из таблицы account_balance_call, у которой колонки:
 * - ИД счета, для которого адресован вызов (PK) - по этой колонке выполняем партиционирование
 * - ИД вызова
 * - timestamp
 * - имя вызываемого метода
 * - параметры
 * - результат выполнения
 * <p>
 * Смысл в этой таблице с запросами в том, чтобы упорядочить запросы к AccountBalanceManager-ам, которых может быть несколько экземпляров.
 * Но при этом обычная очередь не подойдет, потому что выполнение метода в AccountService должно быть синхронным, а следовательно,
 * нужно не только отправить запрос в AccountBalanceManager, но и дождаться результата его выполнения.
 * <p>
 * логика работы:
 * - при вызове любого метода у AccountService он создает новую запись в account_balance_call
 * ! важно, что AccountService работает с account_balance_call только через insert и select, но update ему делать нельзя!
 * - далее периодически проверяет, появился ли результат выполнения (который должен проставить AccountBalanceManager)
 * - если появился, то возвращает его
 * - иначе, ждет таймаут и снова проверяет
 * (*) можно подумать, как лучше реализовать ожидание... например, через какой то аналог корутин (Go, Kotlin) или
 * через "подписку на события", которую сделать через очередь.
 * (*) можно сделать макс. время ожидание для случаев, когда на вызов долго не обрабатывается (AccountBalanceManager сломался).
 * А также сделать отдельный сервис, который проставляет результат "Ошибка" для вызовов, которые так и не были обработаны.
 * <p>
 * Также отвечает за
 * - создание AccountBalanceManager-ов и распределение между ними счетов
 * - перебалансировку AccountBalanceManager-ов в случаях, когда какие то выходят из строя, или наоборот создаются новые
 */
public class AccountService {
    private final AccountRepository accountRepo = Configuration.getBean(AccountRepository.class);

    /**
     * Get all accounts identifiers
     *
     * @return set of account identifiers
     */
    public Set<String> getAccounts() {
        return accountRepo.findAll();
    }

    /**
     * Create new account
     *
     * @return the created account
     */
    public Account createNewAccount() {
        var account = accountRepo.createNew();

        // todo: назначает AccountBalanceManager-а, который отвечает за этот счет

        return account;
    }

    /**
     * Get available account balance
     *
     * @param accountId the account id
     * @return current account balance minus all reserved amounts
     */
    public double getAccountBalance(@NonNull String accountId) {

        // todo: зарегистрировать вызов к AccountBalanceManager-у, ждать результат выполнения
        return 0;
    }

    /**
     * Increase account balance by the amount
     *
     * @param accountId the account id
     * @param amount    amount by which the balance will be increased
     */
    public void creditTheAccount(@NonNull String accountId, double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be positive! amount = " + amount);

        // todo: зарегистрировать вызов к AccountBalanceManager-у, ждать результат выполнения
    }

    /**
     * Decrease account balance by the amount
     *
     * @param accountId the account id
     * @param amount    amount by which the balance will be decreased
     */
    public void debitTheAccount(@NonNull String accountId, double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be positive! amount = " + amount);

        // todo: зарегистрировать вызов к AccountBalanceManager-у, ждать результат выполнения
    }
}
