package com.neverless;

import com.neverless.service.AccountService;
import com.neverless.store.InMemoryAccountStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.http.impl.ReqImpl;
import org.rapidoid.http.impl.RespImpl;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AccountHandlerTest {
    private AccountService accountService;
    private AccountHandler accountHandler;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(new InMemoryAccountStore());
        accountHandler = new AccountHandler(accountService);
    }

    @Test
    void testHandleAccountCreationRequest_Success() {
        Req req = mock(ReqImpl.class);
        Resp resp = mock(RespImpl.class);

        when(req.posted("accountId")).thenReturn("account1");
        when(req.posted("userId")).thenReturn("user1");
        when(req.posted("initialBalance")).thenReturn("1000");

        Map<String, Object> response = accountHandler.handleAccountCreationRequest(req, resp);

        assertEquals("SUCCESS", response.get("status"));
        assertEquals("Account created successfully", response.get("message"));
        verify(resp).code(200);
    }

    @Test
    void testHandleAccountCreationRequest_DuplicateAccount() {
        Req req = mock(ReqImpl.class);
        Resp resp = mock(RespImpl.class);

        when(req.posted("accountId")).thenReturn("account1");
        when(req.posted("userId")).thenReturn("user1");
        when(req.posted("initialBalance")).thenReturn("1000");

        // Create the first account
        accountHandler.handleAccountCreationRequest(req, resp);

        // Try to create the same account again
        Map<String, Object> response = accountHandler.handleAccountCreationRequest(req, resp);

        assertEquals("FAILURE", response.get("status"));
        assertEquals("Account already exists", response.get("message"));
        verify(resp).code(400);
    }
}

