package com.neverless;

import com.neverless.model.Amount;
import com.neverless.model.TransferProgress;
import com.neverless.model.TransferResult;
import com.neverless.service.TransferService;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TransferHandler {
    private final TransferService transferService;

    public TransferHandler(TransferService transferService) {
        this.transferService = transferService;
    }

    public Map<String, Object> handleTransferRequest(Req req, Resp resp) {
        String fromAccountId = req.posted("fromAccountId");
        String toAccountId = req.posted("toAccountId");
        String amountStr = req.posted("amount");
        BigDecimal amountValue = new BigDecimal(amountStr);
        Amount amount = new Amount(amountValue);

        TransferResult result = transferService.transfer(fromAccountId, toAccountId, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("status", result.getStatus().name());
        response.put("message", result.getMessage());
        response.put("taskId", result.getTaskId());
        resp.code(getResponseCode(result));

        return response;
    }

    public Map<String, Object> handleExternalTransferRequest(Req req, Resp resp) {
        String fromAccountId = req.posted("fromAccountId");
        String externalAddress = req.posted("externalAddress");
        String amountStr = req.posted("amount");
        BigDecimal amountValue = new BigDecimal(amountStr);
        Amount amount = new Amount(amountValue);

        TransferResult result = transferService.externalTransfer(fromAccountId, externalAddress, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("status", result.getStatus().name());
        response.put("message", result.getMessage());
        response.put("taskId", result.getTaskId());
        resp.code(getResponseCode(result));

        return response;
    }

    public Map<String, Object> handleProgressRequest(Req req, Resp resp) {
        String transferId = req.param("transferId");
        TransferProgress progress = transferService.getTransferProgress(transferId);

        Map<String, Object> response = new HashMap<>();
        response.put("transferId", progress.getTransferId());
        response.put("status", progress.getStatus().name());
        resp.code(200);

        return response;
    }

    private int getResponseCode(TransferResult result) {
        if (result.getStatus() == TransferResult.Status.SUCCESS) {
            return 200; // OK
        } else if (result.getErrorCode() == TransferResult.ErrorCode.TIMEOUT) {
            return 408; // Request Timeout
        } else if (result.getErrorCode() == TransferResult.ErrorCode.INSUFFICIENT_FUNDS) {
            return 400; // Bad Request
        } else if (result.getErrorCode() == TransferResult.ErrorCode.INVALID_ACCOUNT) {
            return 404; // Not Found
        } else {
            return 500; // Internal Server Error
        }
    }
}
