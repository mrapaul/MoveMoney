package com.neverless.model;

public final class TransferProgress {
    private final String transferId;
    private TransferStatus status;

    public TransferProgress(String transferId, TransferStatus status) {
        this.transferId = transferId;
        this.status = status;
    }

    public String getTransferId() {
        return transferId;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }
}
