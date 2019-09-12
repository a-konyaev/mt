package ru.mt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mt.app.Configuration;
import ru.mt.errors.AccountNotExistException;

import java.util.UUID;

class AccountServiceTest {
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = Configuration.getComponent(AccountService.class);
    }

    @AfterEach
    void tearDown() {
        Configuration.reset();
    }

    @Test
    void createNewAccount() {
        var accountIdsBefore = accountService.getAccounts();

        var newAccountId = accountService.createNewAccount();

        var accountIdsAfter = accountService.getAccounts();

        Assertions.assertEquals(accountIdsBefore.size() + 1, accountIdsAfter.size());
        Assertions.assertFalse(accountIdsBefore.contains(newAccountId));
        Assertions.assertTrue(accountIdsAfter.contains(newAccountId));
    }

    @Test
    void getNonexistentAccount() {
        var randomId = UUID.randomUUID().toString();
        var exception = Assertions.assertThrows(
                AccountNotExistException.class,
                () -> accountService.getAccountBalance(randomId));
        Assertions.assertEquals(randomId, exception.getAccountId());
    }

    @Test
    void getBalanceForNewlyCreatedAccount() {
        var accountId = accountService.createNewAccount();
        var balance = accountService.getAccountBalance(accountId);
        Assertions.assertEquals(0, balance);
    }

//    @Test
//    void creditNonPositiveAmountToAccount() {
//        var accountId = accountService.createNewAccount();
//        Assertions.assertThrows(
//                IllegalArgumentException.class,
//                () -> accountService.addAmount(accountId, 0));
//        Assertions.assertThrows(
//                IllegalArgumentException.class,
//                () -> accountService.addAmount(accountId, -123.45));
//    }
//
//    @Test
//    void creditTheAccount() {
//        var accountId = accountService.createNewAccount();
//        accountService.addAmount(accountId, 100);
//        Assertions.assertEquals(100, accountService.getAccountBalance(accountId));
//        accountService.addAmount(accountId, 77);
//        Assertions.assertEquals(77, accountService.getAccountBalance(accountId));
//    }
//
//    @Test
//    void debitNewlyCreatedAccount() {
//        var accountId = accountService.createNewAccount();
//        // now the balance is zero
//        var exception = Assertions.assertThrows(
//                InsufficientBalanceAccountException.class,
//                () -> accountService.debitTheAccount(accountId, 321));
//        Assertions.assertEquals(accountId, exception.getAccountId());
//        Assertions.assertEquals(0, exception.getAvailableBalance());
//        Assertions.assertEquals(321, exception.getRequestedAmount());
//    }
}