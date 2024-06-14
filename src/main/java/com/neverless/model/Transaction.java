package com.neverless.model;

public record Transaction(String transactionId,
                          String fromAccountId,
                          String toAccountId,
                          Amount amount,
                          String status,
                          String previousTransactionId) {

}

