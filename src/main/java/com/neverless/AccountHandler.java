package com.neverless;

import com.neverless.service.AccountService;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;

import java.util.HashMap;
import java.util.Map;

public class AccountHandler {
    private final AccountService accountService;

    public AccountHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    public Map<String, Object> handleBalanceRequest(Req req, Resp resp) {
        String accountId = req.param("accountId");
        Map<String, Object> response = new HashMap<>();
        try {
            double balance = accountService.getAccountBalance(accountId).doubleValue();
            response.put("status", "SUCCESS");
            response.put("balance", balance);
            resp.code(200);
        } catch (IllegalArgumentException e) {
            response.put("status", "FAILURE");
            response.put("message", e.getMessage());
            resp.code(404);
        }
        return response;
    }

    public Map<String, Object> handleAccountCreationRequest(Req req, Resp resp) {
        String accountId = req.posted("accountId");
        String userId = req.posted("userId");
        double initialBalance = Double.parseDouble(req.posted("initialBalance"));

        Map<String, Object> response = new HashMap<>();
        try {
            accountService.createAccount(accountId, userId, initialBalance);
            response.put("status", "SUCCESS");
            response.put("message", "Account created successfully");
            resp.code(200);
        } catch (IllegalArgumentException e) {
            response.put("status", "FAILURE");
            response.put("message", e.getMessage());
            resp.code(400);
        }

        return response;
    }
}
