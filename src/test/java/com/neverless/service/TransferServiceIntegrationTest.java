package com.neverless.service;

import com.neverless.model.Amount;
import com.neverless.model.TransferProgress;
import com.neverless.model.TransferResult;
import com.neverless.model.TransferStatus;
import com.neverless.store.InMemoryAccountStore;
import com.neverless.store.InMemoryTransactionStore;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransferServiceIntegrationTest {

    private static InMemoryAccountStore accountStore;
    private static TransferQueue transferQueue;
    private static InMemoryTransactionStore transactionStore;
    private static TransferService transferService;
    private static String account1;
    private static String account2;
    private static WithdrawalService withdrawalService;

    @BeforeAll
    static void setup() {
        accountStore = new InMemoryAccountStore();
        transferQueue = new TransferQueue(1024);
        transactionStore = new InMemoryTransactionStore();
        withdrawalService = mock(WithdrawalService.class);
        transferService = new TransferService(accountStore, transactionStore, transferQueue, withdrawalService);

        // Create accounts
        account1 = "account1";
        account2 = "account2";
        accountStore.createAccount(account1, "user1", 1000.0);
        accountStore.createAccount(account2, "user2", 1000.0);
    }

    @AfterEach
    void resetAccounts() {
        accountStore.createAccount(account1, "user1", 1000.0);
        accountStore.createAccount(account2, "user2", 1000.0);
    }

    @AfterAll
    static void teardown() {
        transferQueue.shutdown();
    }

    @Test
    @Order(1)
    void testSuccessfulTransfer() {
        Amount amount = new Amount(new BigDecimal("100.0"));

        TransferResult result = transferService.transfer(account1, account2, amount);

        assertEquals(TransferResult.Status.SUCCESS, result.getStatus());
        assertEquals("Transfer successful", result.getMessage());
        assertNotNull(result.getTaskId());
        assertEquals(900.0, transferService.getAccountBalance(account1));
        assertEquals(1100.0, transferService.getAccountBalance(account2));

        TransferProgress progress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.COMPLETED, progress.getStatus());
    }

    @Test
    @Order(2)
    void testInsufficientFunds() {
        Amount amount = new Amount(new BigDecimal("2000.0")); // More than the balance

        TransferResult result = transferService.transfer(account1, account2, amount);

        assertEquals(TransferResult.Status.FAILURE, result.getStatus());
        assertEquals("Insufficient funds", result.getMessage());
        assertNotNull(result.getTaskId());
        assertEquals(1000.0, transferService.getAccountBalance(account1)); // Balance should remain the same
        assertEquals(1000.0, transferService.getAccountBalance(account2)); // Balance should remain the same

        TransferProgress progress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.FAILED, progress.getStatus());
    }

    @Test
    @Order(3)
    void testInvalidAccount() {
        String invalidAccountId = "invalidAccount"; // Non-existent account
        Amount amount = new Amount(new BigDecimal("100.0"));

        TransferResult result = transferService.transfer(invalidAccountId, account2, amount);

        assertEquals(TransferResult.Status.FAILURE, result.getStatus());
        assertEquals("Invalid account ID", result.getMessage());
        assertNotNull(result.getTaskId());
        assertEquals(1000.0, transferService.getAccountBalance(account1)); // Balance should remain the same
        assertEquals(1000.0, transferService.getAccountBalance(account2)); // Balance should remain the same

        TransferProgress progress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.FAILED, progress.getStatus());
    }

    @Test
    @Order(4)
    void testSuccessfulExternalTransfer() {
        Amount amount = new Amount(new BigDecimal("100.0"));
        String externalAddress = "externalAddress";

        when(withdrawalService.getRequestState(any())).thenReturn(WithdrawalService.WithdrawalState.COMPLETED);

        TransferResult result = transferService.externalTransfer(account1, externalAddress, amount);

        assertEquals(TransferResult.Status.SUCCESS, result.getStatus());
        assertEquals("Transfer successful", result.getMessage());
        assertNotNull(result.getTaskId());
        assertEquals(900.0, transferService.getAccountBalance(account1));

        TransferProgress progress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.COMPLETED, progress.getStatus());
    }

    @Test
    @Order(5)
    void testExternalTransferInsufficientFunds() {
        Amount amount = new Amount(new BigDecimal("2000.0")); // More than the balance
        String externalAddress = "externalAddress";

        TransferResult result = transferService.externalTransfer(account1, externalAddress, amount);

        assertEquals(TransferResult.Status.FAILURE, result.getStatus());
        assertEquals("Insufficient funds", result.getMessage());
        assertNotNull(result.getTaskId());
        assertEquals(1000.0, transferService.getAccountBalance(account1)); // Balance should remain the same

        TransferProgress progress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.FAILED, progress.getStatus());
    }

    @Test
    @Order(6)
    void testExternalTransferInvalidAccount() {
        String invalidAccountId = "invalidAccount"; // Non-existent account
        Amount amount = new Amount(new BigDecimal("100.0"));
        String externalAddress = "externalAddress";

        TransferResult result = transferService.externalTransfer(invalidAccountId, externalAddress, amount);

        assertEquals(TransferResult.Status.FAILURE, result.getStatus());
        assertEquals("Invalid account ID", result.getMessage());
        assertNotNull(result.getTaskId());

        TransferProgress progress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.FAILED, progress.getStatus());
    }

    @Test
    @Order(7)
    void testConcurrency() throws InterruptedException {
        Amount amount = new Amount(new BigDecimal("10.0"));

        int numberOfThreads = 50;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                transferService.transfer(account1, account2, amount);
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        double fromAccountBalance = transferService.getAccountBalance(account1);
        double toAccountBalance = transferService.getAccountBalance(account2);

        double value = amount.getValue().doubleValue();

        assertEquals(1000.0 - numberOfThreads * value, fromAccountBalance, 0.01);
        assertEquals(1000.0 + numberOfThreads * value, toAccountBalance, 0.01);
    }

    @Test
    @Order(8)
    void testProgressUpdates() {
        Amount amount = new Amount(new BigDecimal("100.0"));

        TransferResult result = transferService.transfer(account1, account2, amount);
        assertNotNull(result.getTaskId());

        // Final progress should be COMPLETED
        TransferProgress finalProgress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.COMPLETED, finalProgress.getStatus());
    }

    @Test
    @Order(9)
    void testProgressUpdatesForFailedTransfer() {
        Amount amount = new Amount(new BigDecimal("2000.0")); // More than the balance

        TransferResult result = transferService.transfer(account1, account2, amount);
        assertNotNull(result.getTaskId());

        TransferProgress finalProgress = transferService.getTransferProgress(result.getTaskId());
        assertEquals(TransferStatus.FAILED, finalProgress.getStatus());
    }
}
