package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.repository.AccountsRepositoryInMemory;
import com.google.testing.threadtester.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Threading test class to check that transfer operation in DAO is working and no deadlocks occurs
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsRepositoryInMemoryTest {

    private AccountsRepositoryInMemory accountsRepositoryInMemory;

    // Account IDs for transfer tests
    private static final String SOURCE_ACCOUNT = "sourceAccount";
    private static final String DESTINATION_ACCOUNT = "destinationAccount";

    @ThreadedBefore
    public void before() {
        accountsRepositoryInMemory = new AccountsRepositoryInMemory();

        // Accounts for transfer tests
        accountsRepositoryInMemory.createAccount(new Account(SOURCE_ACCOUNT, new BigDecimal("100")));
        accountsRepositoryInMemory.createAccount(new Account(DESTINATION_ACCOUNT, new BigDecimal("100")));
    }

    @ThreadedMain
    public void mainThread() {
        accountsRepositoryInMemory.transfer(Transfer.builder()
                .sourceAccountId(SOURCE_ACCOUNT)
                .destinationAccountId(DESTINATION_ACCOUNT)
                .amount(new BigDecimal("25"))
                .build());
    }

    @ThreadedSecondary
    public void secondThread() {
        accountsRepositoryInMemory.transfer(Transfer.builder()
                .sourceAccountId(SOURCE_ACCOUNT)
                .destinationAccountId(DESTINATION_ACCOUNT)
                .amount(new BigDecimal("25"))
                .build());
    }

    @ThreadedAfter
    public void after() {
        assertThat(accountsRepositoryInMemory.getAccount(SOURCE_ACCOUNT).getBalance()).isEqualByComparingTo("50");
        assertThat(accountsRepositoryInMemory.getAccount(DESTINATION_ACCOUNT).getBalance()).isEqualByComparingTo("150");
    }

    @Test
    public void testTransfer() {
        new AnnotatedTestRunner().runTests(this.getClass(), AccountsRepositoryInMemory.class);
    }
}
