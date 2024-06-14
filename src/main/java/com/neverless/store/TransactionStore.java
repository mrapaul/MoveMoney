package com.neverless.store;

import com.neverless.model.Transaction;

public interface TransactionStore {
    void logTransaction(Transaction transaction);
    Iterable<Transaction> getTransactionLog();
}
