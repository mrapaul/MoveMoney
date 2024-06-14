package com.neverless.service;

import com.neverless.model.*;
import com.neverless.store.AccountStore;
import com.neverless.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

public class TransferService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferService.class);
    private final AccountStore accountStore;
    private final TransferQueue transferQueue;
    private final TransactionStore transactionStore;
    private final WithdrawalService withdrawalService;
    private final ScheduledExecutorService scheduler;

    public TransferService(AccountStore accountStore, TransactionStore transactionStore, TransferQueue transferQueue, WithdrawalService withdrawalService) {
        this.accountStore = accountStore;
        this.transferQueue = transferQueue;
        this.transactionStore = transactionStore;
        this.withdrawalService = withdrawalService;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public TransferResult transfer(String fromAccountId, String toAccountId, Amount amount) {
        TransferTask task = new TransferTask(fromAccountId, toAccountId, amount, this);
        return transferQueue.submitTask(task);
    }

    public TransferResult externalTransfer(String fromAccountId, String externalAddress, Amount amount) {
        ExternalTransferTask task = new ExternalTransferTask(fromAccountId, externalAddress, amount, this);
        return transferQueue.submitTask(task);
    }

    public TransferProgress getTransferProgress(String transferId) {
        return transferQueue.getTransferProgress(transferId);
    }

    public TransferResult executeTransfer(String taskId, String fromAccountId, String toAccountId, Amount amount) {
        Transaction transaction = new Transaction(taskId, fromAccountId, toAccountId, amount, "PROCESSING", null);
        transactionStore.logTransaction(transaction);

        Account fromAccount = accountStore.getAccount(fromAccountId);
        Account toAccount = accountStore.getAccount(toAccountId);

        if (fromAccount == null || toAccount == null) {
            String message = "Invalid account ID";
            logTransaction(taskId, fromAccountId, toAccountId, amount, "FAILURE", message, "INTERNAL");
            return TransferResult.failure(message, taskId, TransferResult.ErrorCode.INVALID_ACCOUNT);
        }

        if (fromAccount.getBalance().compareTo(amount.getValue()) < 0) {
            String message = "Insufficient funds";
            logTransaction(taskId, fromAccountId, toAccountId, amount, "FAILURE", message, "INTERNAL");
            return TransferResult.failure(message, taskId, TransferResult.ErrorCode.INSUFFICIENT_FUNDS);
        }

        try {
            debit(fromAccount, amount);
            credit(toAccount, amount);
            accountStore.updateAccount(fromAccount);
            accountStore.updateAccount(toAccount);
            logTransaction(taskId, fromAccountId, toAccountId, amount, "SUCCESS", "Transfer successful", "INTERNAL");
            return TransferResult.success(taskId);
        } catch (Exception e) {
            rollback(fromAccount, toAccount, amount);
            String message = "Transfer failed: " + e.getMessage();
            logTransaction(taskId, fromAccountId, toAccountId, amount, "FAILURE", message, "INTERNAL");
            return TransferResult.failure(message, taskId, TransferResult.ErrorCode.UNKNOWN);
        }
    }

    public TransferResult executeExternalTransfer(String taskId, String fromAccountId, String externalAddress, Amount amount) {
        Transaction transaction = new Transaction(taskId, fromAccountId, externalAddress, amount, "PROCESSING", null);
        transactionStore.logTransaction(transaction);

        Account fromAccount = accountStore.getAccount(fromAccountId);

        if (fromAccount == null) {
            String message = "Invalid account ID";
            logTransaction(taskId, fromAccountId, externalAddress, amount, "FAILURE", message, "EXTERNAL");
            return TransferResult.failure(message, taskId, TransferResult.ErrorCode.INVALID_ACCOUNT);
        }

        if (fromAccount.getBalance().compareTo(amount.getValue()) < 0) {
            String message = "Insufficient funds";
            logTransaction(taskId, fromAccountId, externalAddress, amount, "FAILURE", message, "EXTERNAL");
            return TransferResult.failure(message, taskId, TransferResult.ErrorCode.INSUFFICIENT_FUNDS);
        }

        try {
            debit(fromAccount, amount);
            accountStore.updateAccount(fromAccount);
            logTransaction(taskId, fromAccountId, externalAddress, amount, "SUCCESS", "Transfer successful", "EXTERNAL");

            // Initiate withdrawal
            WithdrawalService.WithdrawalId withdrawalId = new WithdrawalService.WithdrawalId(UUID.randomUUID());
            withdrawalService.requestWithdrawal(withdrawalId, new WithdrawalService.Address(externalAddress), amount);

            return waitForWithdrawalCompletion(taskId, fromAccount, externalAddress, amount, withdrawalId);
        } catch (Exception e) {
            rollback(fromAccount, null, amount);
            String message = "Transfer failed: " + e.getMessage();
            logTransaction(taskId, fromAccountId, externalAddress, amount, "FAILURE", message, "EXTERNAL");
            return TransferResult.failure(message, taskId, TransferResult.ErrorCode.UNKNOWN);
        }
    }

    private TransferResult waitForWithdrawalCompletion(String taskId, Account fromAccount, String externalAddress, Amount amount, WithdrawalService.WithdrawalId withdrawalId) {
        CompletableFuture<TransferResult> future = new CompletableFuture<>();

        scheduler.schedule(() -> {
            try {
                while (true) {
                    WithdrawalService.WithdrawalState state = withdrawalService.getRequestState(withdrawalId);
                    if (state == WithdrawalService.WithdrawalState.COMPLETED) {
                        future.complete(TransferResult.success(taskId));
                        break;
                    } else if (state == WithdrawalService.WithdrawalState.FAILED) {
                        rollback(fromAccount, null, amount);
                        future.complete(TransferResult.failure("External transfer failed", taskId, TransferResult.ErrorCode.EXTERNAL_TRANSFER_FAILED));
                        break;
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                rollback(fromAccount, null, amount);
                future.complete(TransferResult.failure("External transfer failed: " + e.getMessage(), taskId, TransferResult.ErrorCode.UNKNOWN));
            }
        }, 0, TimeUnit.SECONDS);

        try {
            return future.get(1, TimeUnit.SECONDS); // future improvement
        } catch (TimeoutException e) {
            rollback(fromAccount, null, amount);
            return TransferResult.failure("Transfer timed out", taskId, TransferResult.ErrorCode.TIMEOUT);
        } catch (Exception e) {
            rollback(fromAccount, null, amount);
            return TransferResult.failure("External transfer failed: " + e.getMessage(), taskId, TransferResult.ErrorCode.UNKNOWN);
        }
    }

    public void updateProgress(String transferId, TransferStatus status) {
        transferQueue.updateProgress(transferId, status);
    }

    private void credit(Account account, Amount amount) {
        BigDecimal newBalance = account.getBalance().add(amount.getValue());
        account.setBalance(newBalance);
    }

    private void debit(Account account, Amount amount) {
        BigDecimal newBalance = account.getBalance().subtract(amount.getValue());
        account.setBalance(newBalance);
    }

    private void rollback(Account fromAccount, Account toAccount, Amount amount) {
        if (fromAccount != null) {
            fromAccount.setBalance(fromAccount.getBalance().add(amount.getValue()));
            accountStore.updateAccount(fromAccount);
        }

        if (toAccount != null) {
            toAccount.setBalance(toAccount.getBalance().subtract(amount.getValue()));
            accountStore.updateAccount(toAccount);
        }
    }

    public double getAccountBalance(String accountId) {
        Account account = accountStore.getAccount(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Invalid account ID");
        }
        return account.getBalance().doubleValue();
    }

    private void logTransaction(String taskId, String fromAccountId, String toAccountId, Amount amount, String status, String message, String type) {
        Transaction transaction = new Transaction(taskId, fromAccountId, toAccountId, amount, status, null);
        transactionStore.logTransaction(transaction);
        LOGGER.debug(String.format("Transaction log: %s | Status: %s | From: %s | To: %s | Amount: %.2f%n",
                message, status, fromAccountId, toAccountId, amount.getValue()));
    }
}
