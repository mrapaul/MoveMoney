package com.neverless.service;

import com.neverless.model.Amount;
import com.neverless.model.TransferProgress;
import com.neverless.model.TransferResult;
import com.neverless.model.TransferStatus;

import java.util.UUID;

public final class TransferTask implements MoneyTransferTask {
    private final String id;
    private final String fromAccountId;
    private final String toAccountId;
    private final Amount amount;
    private final TransferService transferService;
    private final TransferProgress progress;

    public TransferTask(String fromAccountId, String toAccountId, Amount amount, TransferService transferService) {
        this.id = UUID.randomUUID().toString();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.transferService = transferService;
        this.progress = new TransferProgress(id, TransferStatus.INITIATED);
    }

    public String getId() {
        return id;
    }

    public TransferProgress getProgress() {
        return progress;
    }

    @Override
    public TransferResult call() {
        transferService.updateProgress(id, TransferStatus.PROCESSING);
        TransferResult result = transferService.executeTransfer(id, fromAccountId, toAccountId, amount);
        transferService.updateProgress(id, result.getStatus() == TransferResult.Status.SUCCESS ? TransferStatus.COMPLETED : TransferStatus.FAILED);
        return result;
    }
}
