package com.neverless.store;

import com.neverless.model.Account;

public interface AccountStore {
    Account getAccount(String accountId);

    void createAccount(String accountId, String userId, double initialBalance);

    void updateAccount(Account account);
}
