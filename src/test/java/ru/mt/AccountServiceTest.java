package ru.mt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mt.app.Configuration;


class AccountServiceTest {

    @Test
    void createNewAccount() {
        var accountService = Configuration.getBean(AccountService.class);
        var accountIdsBefore = accountService.getAccounts();

        var newAccount = accountService.createNewAccount();
        var newAccountId = newAccount.getId();

        var accountIdsAfter = accountService.getAccounts();

        Assertions.assertEquals(accountIdsBefore.size() + 1, accountIdsAfter.size());
        Assertions.assertFalse(accountIdsBefore.contains(newAccountId));
        Assertions.assertTrue(accountIdsAfter.contains(newAccountId));
    }

    @Test
    void creditTheAccount() {
    }

    @Test
    void debitTheAccount() {
    }

    @Test
    void getAccountBalance() {
    }
}