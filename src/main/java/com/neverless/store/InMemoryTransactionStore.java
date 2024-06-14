package com.neverless.store;

import com.neverless.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryTransactionStore implements TransactionStore {
    private final List<Transaction> transactions;
    private String lastTransactionId = null;

    public InMemoryTransactionStore() {
        transactions = new ArrayList<>();
    }

    @Override
    public void logTransaction(Transaction transaction) {
        transactions.add(transaction);
        lastTransactionId = transaction.transactionId();
    }

    @Override
    public List<Transaction> getTransactionLog() {
        return new ArrayList<>(transactions);
    }

    public String getLastTransactionId() {
        return lastTransactionId;
    }
}
