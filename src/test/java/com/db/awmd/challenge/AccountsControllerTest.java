package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.service.AccountsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    // Object mapper to serialize objects for use in transfer request bodies
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Account IDs for transfer tests
    private static final String SOURCE_ACCOUNT = "sourceAccount";
    private static final String DESTINATION_ACCOUNT = "destinationAccount";

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();

        // Accounts for transfer tests
        accountsService.createAccount(new Account(SOURCE_ACCOUNT, new BigDecimal("100")));
        accountsService.createAccount(new Account(DESTINATION_ACCOUNT, new BigDecimal("100")));
    }

    @Test
    public void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    public void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    /**
     * Test transfer is done successfully according to provided business rules
     *
     * @throws Exception
     */
    @Test
    public void transfer() throws Exception {
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(
                                Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("50"))
                                        .build())))
                .andExpect(status().isOk());

        // Source account: 100 - 50
        Account sourceAccount = accountsService.getAccount(SOURCE_ACCOUNT);
        assertThat(sourceAccount.getAccountId()).isEqualTo(SOURCE_ACCOUNT);
        assertThat(sourceAccount.getBalance()).isEqualByComparingTo("50");

        // Destination account: 100 + 50
        Account destinationAccount = accountsService.getAccount(DESTINATION_ACCOUNT);
        assertThat(destinationAccount.getAccountId()).isEqualTo(DESTINATION_ACCOUNT);
        assertThat(destinationAccount.getBalance()).isEqualByComparingTo("150");
    }

    /**
     * Check that HttpStatus.BAD_REQUEST is returned when no body is sent
     *
     * @throws Exception
     */
    @Test
    public void transferNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Check that HttpStatus.BAD_REQUEST is returned when source and/or destination account don't exist
     *
     * @throws Exception
     */
    @Test
    public void transferNotExistingAccounts() throws Exception {
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(
                                Transfer.builder()
                                        .sourceAccountId("nonExistingSourceAccount")
                                        .destinationAccountId("nonExistingDestinationAccount")
                                        .amount(new BigDecimal("50"))
                                        .build())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Origin and/or destination account does not exists")));
    }

    /**
     * Check that HttpStatus.BAD_REQUEST is returned when the source account has not enough balance
     *
     * @throws Exception
     */
    @Test
    public void transferFromNotEnoughBalanceAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(
                                Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("500"))
                                        .build())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("The source account " + SOURCE_ACCOUNT + " has not enough balance!")));
    }

    /**
     * Check that HttpStatus.BAD_REQUEST is returned when a zero amount is trying to be transferred
     *
     * @throws Exception
     */
    @Test
    public void transferZero() throws Exception {
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(
                                Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("0"))
                                        .build())))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(((MethodArgumentNotValidException) result.getResolvedException())
                        .getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .anyMatch(s -> s.contains("Transfer amount must be greater than 0"))));
    }

    /**
     * Check that HttpStatus.BAD_REQUEST is returned when a negative amount is trying to be transferred
     *
     * @throws Exception
     */
    @Test
    public void transferNegative() throws Exception {
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(
                                Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(DESTINATION_ACCOUNT)
                                        .amount(new BigDecimal("-50"))
                                        .build())))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(((MethodArgumentNotValidException) result.getResolvedException())
                        .getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .anyMatch(s -> s.contains("Transfer amount must be greater than 0"))));
    }

    /**
     * Check that HttpStatus.BAD_REQUEST is returned when source and destination accounts are the same one
     *
     * @throws Exception
     */
    @Test
    public void transferSameAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(
                                Transfer.builder()
                                        .sourceAccountId(SOURCE_ACCOUNT)
                                        .destinationAccountId(SOURCE_ACCOUNT)
                                        .amount(new BigDecimal("50"))
                                        .build())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Origin and destination account cannot be equal")));
    }
}
