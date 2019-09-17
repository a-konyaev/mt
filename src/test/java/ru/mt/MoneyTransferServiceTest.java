package ru.mt;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mt.errors.MoneyTransferException;
import ru.mt.errors.MoneyTransferTransactionException;
import ru.mt.errors.MoneyTransferValidationException;
import ru.mt.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
class MoneyTransferServiceTest extends BaseTest<MoneyTransferService> {
    MoneyTransferServiceTest() {
        super(MoneyTransferService.class);
    }

    @Test
    void checkWrongParameters() {
        var nonExistentAccountId = "nonExistentAccountId";
        var existingAccountId = service.createNewAccount();

        // empty account id
        Assertions.assertThrows(
                MoneyTransferValidationException.class,
                () -> service.transferMoney("", nonExistentAccountId, 1));
        Assertions.assertThrows(
                MoneyTransferValidationException.class,
                () -> service.transferMoney(nonExistentAccountId, "", 1));

        // same account id
        Assertions.assertThrows(
                MoneyTransferValidationException.class,
                () -> service.transferMoney(nonExistentAccountId, nonExistentAccountId, 10));

        // not positive amount
        Assertions.assertThrows(
                MoneyTransferValidationException.class,
                () -> service.putMoneyIntoAccount(nonExistentAccountId, 0));
        Assertions.assertThrows(
                MoneyTransferValidationException.class,
                () -> service.withdrawMoneyFromAccount(nonExistentAccountId, -123.45));

        // account does not exist
        Assertions.assertThrows(
                MoneyTransferTransactionException.class,
                () -> service.putMoneyIntoAccount(nonExistentAccountId, 10));
        Assertions.assertThrows(
                MoneyTransferTransactionException.class,
                () -> service.transferMoney(nonExistentAccountId, existingAccountId, 10));
        Assertions.assertThrows(
                MoneyTransferTransactionException.class,
                () -> service.transferMoney(existingAccountId, nonExistentAccountId, 10));
    }

    @Test
    void checkAccountNotExist() {
        Assertions.assertThrows(
                MoneyTransferException.class,
                () -> service.getAccountBalance("???"));
    }

    @Test
    void createNewAccount() {
        var accountsBefore = service.getAccounts();

        var accountId1 = service.createNewAccount();
        var accountId2 = service.createNewAccount();

        var accountsAfter = service.getAccounts();

        Assertions.assertEquals(accountsBefore.size() + 2, accountsAfter.size());
        Assertions.assertFalse(accountsBefore.containsAll(Set.of(accountId1, accountId2)));
        Assertions.assertTrue(accountsAfter.containsAll(Set.of(accountId1, accountId2)));
    }

    @Test
    @SneakyThrows
    void getAccountBalance() {
        var accountId = service.createNewAccount();
        var balance = service.getAccountBalance(accountId);
        Assertions.assertEquals(0, balance);
    }

    @Test
    @SneakyThrows
    void putMoneyIntoAccount() {
        var accountId = service.createNewAccount();

        service.putMoneyIntoAccount(accountId, 10);
        Assertions.assertEquals(10, service.getAccountBalance(accountId));

        service.putMoneyIntoAccount(accountId, 33);
        Assertions.assertEquals(10 + 33, service.getAccountBalance(accountId));
    }

    @Test
    @SneakyThrows
    void withdrawMoneyFromAccount() {
        var accountId = service.createNewAccount();
        service.putMoneyIntoAccount(accountId, 10);

        service.withdrawMoneyFromAccount(accountId, 3);
        Assertions.assertEquals(10 - 3, service.getAccountBalance(accountId));

        service.withdrawMoneyFromAccount(accountId, 5);
        Assertions.assertEquals(10 - 3 - 5, service.getAccountBalance(accountId));
    }

    @Test
    @SneakyThrows
    void withdrawMoneyFromAccountWhenBalanceIsLow() {
        var accountId = service.createNewAccount();
        service.putMoneyIntoAccount(accountId, 10);

        Assertions.assertThrows(
                MoneyTransferTransactionException.class,
                () -> service.withdrawMoneyFromAccount(accountId, 11));
    }

    @Test
    @SneakyThrows
    void transferMoney() {
        var a1 = service.createNewAccount();
        service.putMoneyIntoAccount(a1, 10);

        var a2 = service.createNewAccount();
        service.transferMoney(a1, a2, 10);

        Assertions.assertEquals(0, service.getAccountBalance(a1));
        Assertions.assertEquals(10, service.getAccountBalance(a2));
    }

    @Test
    @SneakyThrows
    void transferMoneyFromAccountWhenBalanceIsLow() {
        var a1 = service.createNewAccount();
        service.putMoneyIntoAccount(a1, 10);

        var a2 = service.createNewAccount();

        Assertions.assertThrows(
                MoneyTransferTransactionException.class,
                () -> service.transferMoney(a1, a2, 11));
    }

    @Test
    @SneakyThrows
    void parallelTransferMoneyFromOneAccountToAnother() {
        final int COUNT = RandomUtils.getRandomInt(10, 100);
        var accountFrom = service.createNewAccount();
        service.putMoneyIntoAccount(accountFrom, COUNT + 1);

        var finishedLatch = new CountDownLatch(COUNT);
        AtomicInteger errorsCount = new AtomicInteger(0);

        List<String> accountToList = new ArrayList<>(COUNT);

        for (int i = 0; i < COUNT; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(RandomUtils.getRandomInt(10, 100));
                } catch (InterruptedException ignored) {
                }

                var accountTo = service.createNewAccount();
                accountToList.add(accountTo);

                try {
                    service.transferMoney(accountFrom, accountTo, 1);
                } catch (MoneyTransferException e) {
                    errorsCount.incrementAndGet();
                    log.error(e);
                }
                finishedLatch.countDown();
            });
        }

        Assertions.assertTrue(
                finishedLatch.await(300 + COUNT * 10, TimeUnit.MILLISECONDS),
                "Transfer money threads not finishedLatch on time");
        Assertions.assertEquals(0, errorsCount.get());

        Assertions.assertEquals(1, service.getAccountBalance(accountFrom));
        for (String accountTo : accountToList) {
            Assertions.assertEquals(1, service.getAccountBalance(accountTo));
        }
    }

    //todo: сделать эмуляцию долгой транзакции, когда какая то часть денег зарезервирована, но не списана какое то время,
    // и в этот момент запросить баланс - увидеть, что он отражает баланс минус зарезервированная сумма.
    // и попробовать перевести сумму равную балансу - должны получить отказ, т.к. не хватит из-за того, что часть зарезервирована

    //todo: проверить, что при кол-ве шард > 1:
    // 1) один и тот же аккаунт обрабатывается одним и тем же аккайнт-манагером
    // 2) а аккаунты из разных шард - разными
}