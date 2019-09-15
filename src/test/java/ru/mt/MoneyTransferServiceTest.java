package ru.mt;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

@Log4j2
class MoneyTransferServiceTest extends BaseTest<MoneyTransferService> {
    MoneyTransferServiceTest() {
        super(MoneyTransferService.class);
    }

    @Test
    void checkWrongParameters() {
        var accountId = "nonExistentAccountId";

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.transferMoney("", accountId,1));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.transferMoney(accountId, "", 1));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.putMoneyIntoAccount(accountId, 0));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.withdrawMoneyFromAccount(accountId, -123.45));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.transferMoney(accountId, accountId, 10));
    }

    @Test
    void checkAccountNotExist() {
        Assertions.assertThrows(
                IllegalStateException.class,
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
    void getAccountBalance() {
        var accountId = service.createNewAccount();
        var balance = service.getAccountBalance(accountId);
        Assertions.assertEquals(0, balance);
    }

    @Test
    void putMoneyIntoAccount() {
        var accountId = service.createNewAccount();

        service.putMoneyIntoAccount(accountId, 10);
        Assertions.assertEquals(10, service.getAccountBalance(accountId));

        service.putMoneyIntoAccount(accountId, 33);
        Assertions.assertEquals(10 + 33, service.getAccountBalance(accountId));
    }

    @Test
    void withdrawMoneyFromAccount() {
        var accountId = service.createNewAccount();
        service.putMoneyIntoAccount(accountId, 10);

        service.withdrawMoneyFromAccount(accountId, 3);
        Assertions.assertEquals(10 - 3, service.getAccountBalance(accountId));

        service.withdrawMoneyFromAccount(accountId, 5);
        Assertions.assertEquals(10 - 3 - 5, service.getAccountBalance(accountId));
    }

    @Test
    void transferMoney() {
        var a1 = service.createNewAccount();
        service.putMoneyIntoAccount(a1, 10);

        var a2 = service.createNewAccount();
        service.transferMoney(a1, a2, 10);

        Assertions.assertEquals(0, service.getAccountBalance(a1));
        Assertions.assertEquals(10, service.getAccountBalance(a2));
    }

    // перевод денег, когда не хватает средств на счете

    // todo: проверить, как запросы баланса из 2-х разных тредов отработают
    // в части ожидания и получения результата


    //todo: проверить, что при кол-ве шард > 1:
    // 1) один и тот же аккаунт обрабатывается одним и тем же аккайнт-манагером
    // 2) а аккаунты из разных шард - разными

    // несколько потоков переводят деньги с одного счета на дгурие
}