package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.NotEnoughBalanceException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

    /**
     * Mock the NotificationService to be able to test if it has been invoked when performing a transfer
     */
    @MockBean
    private NotificationService notificationService;

    @Autowired
    private AccountsService accountsService;

    // Account IDs for transfer tests
    private static final String SOURCE_ACCOUNT = "sourceAccount";
    private static final String DESTINATION_ACCOUNT = "destinationAccount";

    @Before
    public void prepare() {
        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();

        // Accounts for transfer tests
        accountsService.createAccount(new Account(SOURCE_ACCOUNT, new BigDecimal("100")));
        accountsService.createAccount(new Account(DESTINATION_ACCOUNT, new BigDecimal("100")));
    }

    @Test
    public void addAccount() throws Exception {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    public void addAccount_failsOnDuplicateId() throws Exception {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    /**
     * Test transfer is done successfully according to provided business rules
     *
     * Also checks that when doing a transfer the notifyAboutTransfer method in
     * NotificationService is invoked 2 times to notify both sender and receiver
     */
    @Test
    public void transfer() {
        this.accountsService.transfer(Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("50"))
                                        .build());

        // Source account: 100 - 50
        Account sourceAccount = accountsService.getAccount(SOURCE_ACCOUNT);
        assertThat(sourceAccount.getAccountId()).isEqualTo(SOURCE_ACCOUNT);
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("50");

        // Destination account: 100 + 50
        Account destinationAccount = accountsService.getAccount(DESTINATION_ACCOUNT);
        assertThat(destinationAccount.getAccountId()).isEqualTo(DESTINATION_ACCOUNT);
        assertThat(destinationAccount.getBalance()).isEqualByComparingTo("150");

        Mockito.verify(notificationService, Mockito.times(2)).notifyAboutTransfer(Mockito.anyObject(), Mockito.anyString());
    }

    /**
     * Check that IllegalArgumentException is thrown when source and/or destination account don't exist
     */
    @Test(expected = IllegalArgumentException.class)
    public void transferNotExistingAccounts() {
        this.accountsService.transfer(Transfer.builder()
                                        .sourceAccountId("nonExistingSourceAccount")
                                        .destinationAccountId("nonExistingDestinationAccount")
                                        .amount(new BigDecimal("50"))
                                        .build());
    }

    /**
     * Check that NotEnoughBalanceException is thrown when the source account has not enough balance
     */
    @Test(expected = NotEnoughBalanceException.class)
    public void transferFromNotEnoughBalanceAccount() {
        this.accountsService.transfer(Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("500"))
                                        .build());
    }

    /**
     * Check that IllegalArgumentException is thrown when a zero amount is trying to be transferred
     */
    @Test(expected = IllegalArgumentException.class)
    public void transferZero() throws Exception {
        this.accountsService.transfer(Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("0"))
                                        .build());
    }

    /**
     * Check that IllegalArgumentException is thrown when a negative amount is trying to be transferred
     */
    @Test(expected = IllegalArgumentException.class)
    public void transferNegative()  {
        this.accountsService.transfer(Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("-50"))
                                        .build());
    }

    /**
     * Check that IllegalArgumentException is thrown when source and destination accounts are the same one
     */
    @Test(expected = IllegalArgumentException.class)
    public void transferSameAccount() {
        this.accountsService.transfer(Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(SOURCE_ACCOUNT)
                                        .amount(new BigDecimal("50"))
                                        .build());
    }
}
