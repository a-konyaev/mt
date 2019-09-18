package ru.mt;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.mt.errors.MoneyTransferException;
import ru.mt.errors.MoneyTransferTransactionException;
import ru.mt.errors.MoneyTransferValidationException;
import ru.mt.utils.RandomUtils;

import java.math.BigDecimal;
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

    private static void assertEquals(int expected, BigDecimal actual) {
        Assertions.assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static void assertThrowsMTValidationException(Executable executable) {
        Assertions.assertThrows(MoneyTransferValidationException.class, executable);
    }

    private static void assertThrowsMTTransactionException(Executable executable) {
        Assertions.assertThrows(MoneyTransferTransactionException.class, executable);
    }

    private static void assertThrowsMTException(Executable executable) {
        Assertions.assertThrows(MoneyTransferException.class, executable);
    }


    @Test
    void validateAccountIdParameter() {
        var accountId = service.createNewAccount();

        // empty account id
        assertThrowsMTValidationException(() -> service.transferMoney(null, accountId, BigDecimal.ONE));
        assertThrowsMTValidationException(() -> service.transferMoney("", accountId, BigDecimal.ONE));
        assertThrowsMTValidationException(() -> service.transferMoney(accountId, "", BigDecimal.ONE));

        // same account id
        assertThrowsMTValidationException(() -> service.transferMoney(accountId, accountId, BigDecimal.TEN));
    }

    @Test
    @SneakyThrows
    void validateAmountParameter() {
        var accountId = service.createNewAccount();

        // null amount
        assertThrowsMTValidationException(() -> service.putMoneyIntoAccount(accountId, null));

        // not positive amount
        assertThrowsMTValidationException(() -> service.putMoneyIntoAccount(accountId, BigDecimal.ZERO));
        assertThrowsMTValidationException(() -> service.withdrawMoneyFromAccount(accountId, new BigDecimal("-1")));

        var minAmount = new BigDecimal("0.01");

        // amount above max
        service.putMoneyIntoAccount(accountId, MoneyTransferService.ONE_TRANSACTION_MAX_AMOUNT);
        assertThrowsMTValidationException(() -> service.putMoneyIntoAccount(
                accountId, MoneyTransferService.ONE_TRANSACTION_MAX_AMOUNT.add(minAmount)));

        // amount scale above max
        service.putMoneyIntoAccount(accountId, new BigDecimal("1"));
        service.putMoneyIntoAccount(accountId, new BigDecimal("1.1"));
        service.putMoneyIntoAccount(accountId, new BigDecimal("1.11"));
        service.putMoneyIntoAccount(accountId, new BigDecimal("100.01"));
        service.putMoneyIntoAccount(accountId, minAmount);

        assertThrowsMTValidationException(() -> service.putMoneyIntoAccount(accountId, new BigDecimal("0.009")));
    }

    @Test
    void checkAccountNotExist() {
        var nonExistentAccountId = "<not-exist>";
        var accountId = service.createNewAccount();

        assertThrowsMTException(() -> service.getAccountBalance(nonExistentAccountId));
        assertThrowsMTTransactionException(() -> service.putMoneyIntoAccount(nonExistentAccountId, BigDecimal.TEN));
        assertThrowsMTTransactionException(() -> service.withdrawMoneyFromAccount(nonExistentAccountId, BigDecimal.TEN));
        assertThrowsMTTransactionException(() -> service.transferMoney(nonExistentAccountId, accountId, BigDecimal.TEN));
        assertThrowsMTTransactionException(() -> service.transferMoney(accountId, nonExistentAccountId, BigDecimal.TEN));
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
        assertEquals(0, balance);
    }

    @Test
    @SneakyThrows
    void putMoneyIntoAccount() {
        var accountId = service.createNewAccount();

        service.putMoneyIntoAccount(accountId, BigDecimal.TEN);
        assertEquals(10, service.getAccountBalance(accountId));

        service.putMoneyIntoAccount(accountId, BigDecimal.ONE);
        assertEquals(11, service.getAccountBalance(accountId));
    }

    @Test
    @SneakyThrows
    void withdrawMoneyFromAccount() {
        var accountId = service.createNewAccount();
        service.putMoneyIntoAccount(accountId, BigDecimal.TEN);

        service.withdrawMoneyFromAccount(accountId, new BigDecimal(3));
        assertEquals(10 - 3, service.getAccountBalance(accountId));

        service.withdrawMoneyFromAccount(accountId, new BigDecimal(5));
        assertEquals(10 - 3 - 5, service.getAccountBalance(accountId));
    }

    @Test
    @SneakyThrows
    void withdrawMoneyFromAccountWhenBalanceIsLow() {
        var accountId = service.createNewAccount();
        service.putMoneyIntoAccount(accountId, BigDecimal.TEN);

        Assertions.assertThrows(
                MoneyTransferTransactionException.class,
                () -> service.withdrawMoneyFromAccount(accountId, new BigDecimal(11)));
    }

    @Test
    @SneakyThrows
    void transferMoney() {
        var a1 = service.createNewAccount();
        service.putMoneyIntoAccount(a1, BigDecimal.TEN);

        var a2 = service.createNewAccount();
        service.transferMoney(a1, a2, BigDecimal.TEN);

        assertEquals(0, service.getAccountBalance(a1));
        assertEquals(10, service.getAccountBalance(a2));
    }

    @Test
    @SneakyThrows
    void transferMoneyFromAccountWhenBalanceIsLow() {
        var a1 = service.createNewAccount();
        service.putMoneyIntoAccount(a1, BigDecimal.TEN);

        var a2 = service.createNewAccount();

        Assertions.assertThrows(
                MoneyTransferTransactionException.class,
                () -> service.transferMoney(a1, a2, new BigDecimal(11)));
    }

    @Test
    @SneakyThrows
    void parallelTransferMoneyFromOneAccountToAnother() {
        final int COUNT = RandomUtils.getRandomInt(10, 50);
        var accountFrom = service.createNewAccount();
        service.putMoneyIntoAccount(accountFrom, new BigDecimal(COUNT + 1));

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
                    service.transferMoney(accountFrom, accountTo, BigDecimal.ONE);
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

        assertEquals(1, service.getAccountBalance(accountFrom));
        for (String accountTo : accountToList) {
            assertEquals(1, service.getAccountBalance(accountTo));
        }
    }

    //todo: сделать эмуляцию долгой транзакции, когда какая то часть денег зарезервирована, но не списана какое то время,
    // и в этот момент запросить баланс - увидеть, что он отражает баланс минус зарезервированная сумма.
    // и попробовать перевести сумму равную балансу - должны получить отказ, т.к. не хватит из-за того, что часть зарезервирована

    //todo: проверить, что при кол-ве шард аккаунтов > 1:
    // 1) один и тот же аккаунт обрабатывается одним и тем же AccountBalanceManager-ом
    // 2) а аккаунты из разных шард - разными
}