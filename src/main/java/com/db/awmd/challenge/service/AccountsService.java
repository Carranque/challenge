package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    private final NotificationService notificationService;

//    NOTE: With Lombok, @AllArgsConstructor can be used to generate the constructor and inject the dependency
//    @Autowired
//    public AccountsService(AccountsRepository accountsRepository) {
//        this.accountsRepository = accountsRepository;
//    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    /**
     * Performs the transfer if it's a valid one
     *
     * @param transfer
     */
    public void transfer(final Transfer transfer) {
        List<String> errors = this.validateTransfer(transfer);

        if (errors.isEmpty()) {
            // Perform the transfer
            this.accountsRepository.transfer(transfer);
            // Notify account holders
            this.notifyTransfer(transfer);
        } else {
            throw new IllegalArgumentException("Invalid transfer: " + errors);
        }
    }

    /**
     * Check a transfer object against provided business rules
     *
     * @param transfer
     * @return List of validation issues found
     */
    private List<String> validateTransfer(final Transfer transfer) {
        if (transfer == null) {
            throw new IllegalArgumentException("A transfer should be provided for validation");
        }

        List<String> errors = new ArrayList<>();

        // Transfer from/to an account that does not exist
        if (this.accountsRepository.getAccount(transfer.getSourceAccountId()) == null ||
                this.accountsRepository.getAccount(transfer.getDestinationAccountId()) == null) {
            errors.add("Origin and/or destination account does not exists");
        }
        // Transfer a negative amount or zero (this should be already checked through bean validation at controller level)
        if (transfer.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Transfer amount must be greater than 0");
        }
        // Transfer to the same account
        if (transfer.getSourceAccountId().equals(transfer.getDestinationAccountId())) {
            errors.add("Origin and destination account cannot be equal");
        }
        // Transfer from an account with not enough balance: This validation should be performed thread safe in DAO layer

        return errors;
    }

    /**
     * Notifies source and destination account about a transfer using NotificationService
     *
     * @param transfer
     */
    private void notifyTransfer(final Transfer transfer) {
        Account sourceAccount = this.accountsRepository.getAccount(transfer.getSourceAccountId());
        Account destinationAccount = this.accountsRepository.getAccount(transfer.getSourceAccountId());

        notificationService.notifyAboutTransfer(sourceAccount, String.format("%f send to %s", transfer.getAmount(), destinationAccount.getAccountId()));
        notificationService.notifyAboutTransfer(destinationAccount, String.format("%f received from %s", transfer.getAmount(), sourceAccount.getAccountId()));
    }
}
