package com.neverless.model;

public class TransferResult {
    public enum Status {
        SUCCESS,
        FAILURE
    }

    public enum ErrorCode {
        INSUFFICIENT_FUNDS,
        INVALID_ACCOUNT,
        TIMEOUT,
        EXTERNAL_TRANSFER_FAILED,
        UNKNOWN
    }

    private final Status status;
    private final String message;
    private final String taskId;
    private final ErrorCode errorCode;

    private TransferResult(Status status, String message, String taskId, ErrorCode errorCode) {
        this.status = status;
        this.message = message;
        this.taskId = taskId;
        this.errorCode = errorCode;
    }

    public static TransferResult success(String taskId) {
        return new TransferResult(Status.SUCCESS, "Transfer successful", taskId, null);
    }

    public static TransferResult failure(String message, String taskId, ErrorCode errorCode) {
        return new TransferResult(Status.FAILURE, message, taskId, errorCode);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTaskId() {
        return taskId;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
