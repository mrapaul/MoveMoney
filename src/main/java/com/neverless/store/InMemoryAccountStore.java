package com.neverless.store;

import com.neverless.model.Account;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountStore implements AccountStore {
    private final Map<String, Account> accounts;

    public InMemoryAccountStore() {
        accounts = new ConcurrentHashMap<>();
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void createAccount(String accountId, String userId, double initialBalance) {
        Account account = new Account(accountId, userId, initialBalance);
        accounts.put(accountId, account);
    }

    @Override
    public void updateAccount(Account account) {
        accounts.put(account.getAccountId(), account);
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }
}
