package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.NotEnoughBalanceException;

public interface AccountsRepository {

    void createAccount(Account account) throws DuplicateAccountIdException;

    Account getAccount(String accountId);

    void clearAccounts();

    void transfer(Transfer transfer) throws NotEnoughBalanceException;
}
