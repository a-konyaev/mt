package ru.mt;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

@Log4j2
class MoneyTransferServiceTest extends BaseTest<MoneyTransferService> {
    public MoneyTransferServiceTest() {
        super(MoneyTransferService.class);
    }

    @Test
    void getAccounts() {
        Assertions.assertTrue(service.getAccounts().isEmpty());
    }

    @Test
    void createNewAccount() {
        var accountId1 = service.createNewAccount();
        var accountId2 = service.createNewAccount();
        var accounts = service.getAccounts();

        Assertions.assertEquals(2, accounts.size());
        Assertions.assertTrue(accounts.containsAll(Set.of(accountId1, accountId2)));
    }

    @Test
    void getAccountBalance() {
        var accountId = service.createNewAccount();
        var balance = service.getAccountBalance(accountId);
        Assertions.assertEquals(0, balance);
    }

    @Test
    void putMoneyIntoAccount() {
    }

    @Test
    void withdrawMoneyFromAccount() {
    }

    @Test
    void transferMoney() {
    }
}