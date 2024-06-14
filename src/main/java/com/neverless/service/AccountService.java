package com.neverless.service;

import com.neverless.model.Account;
import com.neverless.store.AccountStore;

import java.math.BigDecimal;

public final class AccountService {
    private final AccountStore accountStore;

    public AccountService(AccountStore accountStore) {
        this.accountStore = accountStore;
    }

    public void createAccount(String accountId, String userId, double initialBalance) {
        if (accountStore.getAccount(accountId) != null) {
            throw new IllegalArgumentException("Account already exists");
        }
        accountStore.createAccount(accountId, userId, initialBalance);
    }
    public BigDecimal getAccountBalance(String accountId) {
        Account account = accountStore.getAccount(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Invalid account ID");
        }
        return account.getBalance();
    }
}
