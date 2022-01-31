package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.NotEnoughBalanceException;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }

    /**
     * Performs the amount transfer between accounts
     *
     * Ideally these operations (balance decrease from source account and balance increase to destination account)
     * would be performed on a database and the method annotated as Transactional, so both or none (in case of
     * any error) should be performed.
     *
     * @param transfer
     * @throws NotEnoughBalanceException
     */
    @Override
    public void transfer(final Transfer transfer) throws NotEnoughBalanceException {
        // Decrease balance from source account
        accounts.compute(transfer.getSourceAccountId(), (sourceAccountId, sourceAccount) -> {
            if (sourceAccount.getBalance().compareTo(transfer.getAmount()) < 0) {
                throw new NotEnoughBalanceException("The source account " + sourceAccountId + " has not enough balance!");
            } else {
                sourceAccount.setBalance(sourceAccount.getBalance().subtract(transfer.getAmount()));
                return sourceAccount;
            }
        });
        // Increase balance on destination account
        accounts.compute(transfer.getDestinationAccountId(), (destinationAccountId, destinationAccount) -> {
            destinationAccount.setBalance(destinationAccount.getBalance().add(transfer.getAmount()));
            return destinationAccount;
        });
    }

}
