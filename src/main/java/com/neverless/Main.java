package com.neverless;

import com.neverless.service.AccountService;
import com.neverless.service.TransferQueue;
import com.neverless.service.TransferService;
import com.neverless.service.WithdrawalServiceStub;
import com.neverless.store.InMemoryAccountStore;
import com.neverless.store.InMemoryTransactionStore;
import org.rapidoid.setup.On;

public class Main {
    private final TransferHandler transferHandler;
    private final AccountHandler accountHandler;

    public Main() {
        var transferQueue = new TransferQueue(1024); // future improvement : configurable
        var accountStore = new InMemoryAccountStore();
        var transactionStore = new InMemoryTransactionStore();
        var withdrawalService = new WithdrawalServiceStub(); // Assume a stub implementation
        var transferService = new TransferService(accountStore, transactionStore, transferQueue, withdrawalService);
        var accountService = new AccountService(accountStore);
        this.transferHandler = new TransferHandler(transferService);
        this.accountHandler = new AccountHandler(accountService);
    }

    public static void main(String[] args) {
        new Main().start();
    }

    private void start() {
        setupTransferEndpoints();
        setupAccountEndpoints();
    }

    private void setupTransferEndpoints() {
        On.post("/transfer").json(transferHandler::handleTransferRequest);
        On.post("/external-transfer").json(transferHandler::handleExternalTransferRequest);
        On.get("/transfer-progress").json(transferHandler::handleProgressRequest);
    }

    private void setupAccountEndpoints() {
        On.post("/create-account").json(accountHandler::handleAccountCreationRequest);
        On.get("/balance").json(accountHandler::handleBalanceRequest);
    }
}
