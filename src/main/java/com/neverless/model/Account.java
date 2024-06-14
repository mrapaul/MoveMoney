package com.neverless.model;

import java.math.BigDecimal;

public final class Account {
    private final String accountId;
    private final String userId;
    private Amount balance;

    public Account(String accountId, String userId, double initialBalance) {
        this.accountId = accountId;
        this.userId = userId;
        this.balance = new Amount(new BigDecimal(initialBalance));
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance.getValue();
    }

    public void setBalance(BigDecimal balance) {
        this.balance = new Amount(balance);
    }
}
