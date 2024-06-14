package com.neverless;

import com.neverless.model.Amount;
import com.neverless.model.TransferResult;
import com.neverless.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TransferHandlerTest {
    private TransferService transferService;
    private TransferHandler transferHandler;

    @BeforeEach
    void setUp() {
        transferService = mock(TransferService.class);
        transferHandler = new TransferHandler(transferService);
    }

    @Test
    void testHandleTransferRequest_Success() {
        Req req = createMockRequest("account1", "account2", "100.0");
        Resp resp = mock(Resp.class);

        TransferResult transferResult = TransferResult.success("task1");
        when(transferService.transfer(eq("account1"), eq("account2"), any(Amount.class)))
                .thenReturn(transferResult);

        Map<String, Object> response = transferHandler.handleTransferRequest(req, resp);

        assertEquals("SUCCESS", response.get("status"));
        assertEquals("Transfer successful", response.get("message"));
        assertEquals("task1", response.get("taskId"));
        verify(resp).code(200);
    }

    @Test
    void testHandleTransferRequest_Failure() {
        Req req = createMockRequest("account1", "account2", "2000.0");
        Resp resp = mock(Resp.class);

        TransferResult transferResult = TransferResult.failure("Insufficient funds", "task1", TransferResult.ErrorCode.INSUFFICIENT_FUNDS);
        when(transferService.transfer(eq("account1"), eq("account2"), any(Amount.class)))
                .thenReturn(transferResult);

        Map<String, Object> response = transferHandler.handleTransferRequest(req, resp);

        assertEquals("FAILURE", response.get("status"));
        assertEquals("Insufficient funds", response.get("message"));
        assertEquals("task1", response.get("taskId"));
        verify(resp).code(400);
    }

    @Test
    void testHandleExternalTransferRequest_Success() {
        Req req = createMockRequest("account1", "externalAddress", "100.0", true);
        Resp resp = mock(Resp.class);

        TransferResult transferResult = TransferResult.success("task1");
        when(transferService.externalTransfer(eq("account1"), eq("externalAddress"), any(Amount.class)))
                .thenReturn(transferResult);

        Map<String, Object> response = transferHandler.handleExternalTransferRequest(req, resp);

        assertEquals("SUCCESS", response.get("status"));
        assertEquals("Transfer successful", response.get("message"));
        assertEquals("task1", response.get("taskId"));
        verify(resp).code(200);
    }

    @Test
    void testHandleExternalTransferRequest_Failure() {
        Req req = createMockRequest("account1", "externalAddress", "2000.0", true);
        Resp resp = mock(Resp.class);

        TransferResult transferResult = TransferResult.failure("Insufficient funds", "task1", TransferResult.ErrorCode.INSUFFICIENT_FUNDS);
        when(transferService.externalTransfer(eq("account1"), eq("externalAddress"), any(Amount.class)))
                .thenReturn(transferResult);

        Map<String, Object> response = transferHandler.handleExternalTransferRequest(req, resp);

        assertEquals("FAILURE", response.get("status"));
        assertEquals("Insufficient funds", response.get("message"));
        assertEquals("task1", response.get("taskId"));
        verify(resp).code(400);
    }

    @Test
    void testHandleExternalTransferRequest_Timeout() {
        Req req = createMockRequest("account1", "externalAddress", "100.0", true);
        Resp resp = mock(Resp.class);

        TransferResult transferResult = TransferResult.failure("Transfer timed out", "task1", TransferResult.ErrorCode.TIMEOUT);
        when(transferService.externalTransfer(eq("account1"), eq("externalAddress"), any(Amount.class)))
                .thenReturn(transferResult);

        Map<String, Object> response = transferHandler.handleExternalTransferRequest(req, resp);

        assertEquals("FAILURE", response.get("status"));
        assertEquals("Transfer timed out", response.get("message"));
        assertEquals("task1", response.get("taskId"));
        verify(resp).code(408); // 408 Request Timeout
    }

    @Test
    void testHandleExternalTransferRequest_WithdrawalFailure() {
        Req req = createMockRequest("account1", "externalAddress", "100.0", true);
        Resp resp = mock(Resp.class);

        TransferResult transferResult = TransferResult.failure("External transfer failed", "task1", TransferResult.ErrorCode.EXTERNAL_TRANSFER_FAILED);
        when(transferService.externalTransfer(eq("account1"), eq("externalAddress"), any(Amount.class)))
                .thenReturn(transferResult);

        Map<String, Object> response = transferHandler.handleExternalTransferRequest(req, resp);

        assertEquals("FAILURE", response.get("status"));
        assertEquals("External transfer failed", response.get("message"));
        assertEquals("task1", response.get("taskId"));
        verify(resp).code(500); // 500 Internal Server Error
    }

    private Req createMockReq(Map<String, String> postedData) {
        Req req = mock(Req.class);
        for (Map.Entry<String, String> entry : postedData.entrySet()) {
            when(req.posted(entry.getKey())).thenReturn(entry.getValue());
        }
        return req;
    }

    private Req createMockRequest(String fromAccountId, String toAccountId, String amount) {
        return createMockRequest(fromAccountId, toAccountId, amount, false);
    }

    private Req createMockRequest(String fromAccountId, String toAccountId, String amount, boolean isExternal) {
        Map<String, String> postedData = new HashMap<>();
        postedData.put("fromAccountId", fromAccountId);
        postedData.put(isExternal ? "externalAddress" : "toAccountId", toAccountId);
        postedData.put("amount", amount);
        return createMockReq(postedData);
    }
}
