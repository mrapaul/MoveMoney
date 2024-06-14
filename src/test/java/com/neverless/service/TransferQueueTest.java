package com.neverless.service;

import com.neverless.model.TransferProgress;
import com.neverless.model.TransferResult;
import com.neverless.model.TransferStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TransferQueueTest {
    private TransferQueue transferQueue;

    @BeforeEach
    void setUp() {
        transferQueue = new TransferQueue(100);
    }

    @AfterEach
    void tearDown() {
        transferQueue.shutdown();
    }

    @Test
    void testSubmitTask_Success() throws ExecutionException, InterruptedException {
        MoneyTransferTask task = new MockMoneyTransferTask("task1");
        TransferResult result = transferQueue.submitTask(task);

        assertEquals(TransferResult.Status.SUCCESS, result.getStatus());
        assertEquals("task1", result.getTaskId());

        TransferProgress progress = transferQueue.getTransferProgress("task1");
        assertEquals(TransferStatus.COMPLETED, progress.getStatus());
    }

    @Test
    void testSubmitTask_Failure() throws ExecutionException, InterruptedException {
        MoneyTransferTask task = new MockMoneyTransferTask("task2", true);
        TransferResult result = transferQueue.submitTask(task);

        assertEquals(TransferResult.Status.FAILURE, result.getStatus());
        assertEquals("task2", result.getTaskId());
        assertEquals("Task failed", result.getMessage());

        TransferProgress progress = transferQueue.getTransferProgress("task2");
        assertEquals(TransferStatus.FAILED, progress.getStatus());
    }

    @Test
    void testGetTransferProgress_UnknownTask() {
        TransferProgress progress = transferQueue.getTransferProgress("unknown");
        assertEquals(TransferStatus.UNKNOWN, progress.getStatus());
    }

    @Test
    void testUpdateProgress() {
        MoneyTransferTask task = new MockMoneyTransferTask("task3");
        transferQueue.submitTask(task);

        transferQueue.updateProgress("task3", TransferStatus.PROCESSING);
        TransferProgress progress = transferQueue.getTransferProgress("task3");
        assertEquals(TransferStatus.PROCESSING, progress.getStatus());

        transferQueue.updateProgress("task3", TransferStatus.COMPLETED);
        progress = transferQueue.getTransferProgress("task3");
        assertEquals(TransferStatus.COMPLETED, progress.getStatus());
    }

    @Test
    void testShutdown() throws InterruptedException {
        MoneyTransferTask task = new MockMoneyTransferTask("task4");
        transferQueue.submitTask(task);

        transferQueue.shutdown();
        assertFalse(transferQueue.submitTask(new MockMoneyTransferTask("task5")).getStatus() == TransferResult.Status.SUCCESS);
    }

    @Test
    void testSubmitTask_AfterShutdown() {
        transferQueue.shutdown();
        MoneyTransferTask task = new MockMoneyTransferTask("task6");
        TransferResult result = transferQueue.submitTask(task);

        assertEquals(TransferResult.Status.FAILURE, result.getStatus());
        assertEquals("Queue is not running", result.getMessage());
    }

    @Test
    void testConcurrentTaskSubmission() throws InterruptedException {
        int numTasks = 100;
        CountDownLatch latch = new CountDownLatch(numTasks);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            executorService.submit(() -> {
                MoneyTransferTask task = new MockMoneyTransferTask("task" + taskId);
                TransferResult result = transferQueue.submitTask(task);
                if (result.getStatus() == TransferResult.Status.SUCCESS) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(numTasks, successCount.get());
    }

    @Test
    void testBackpressureHandling() throws InterruptedException {
        int numTasks = 120; // More than the queue capacity
        CountDownLatch latch = new CountDownLatch(numTasks);
        AtomicInteger failureCount = new AtomicInteger(0);
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            executorService.submit(() -> {
                MoneyTransferTask task = new MockMoneyTransferTask("task" + taskId);
                TransferResult result = transferQueue.submitTask(task);
                if (result.getStatus() == TransferResult.Status.FAILURE) {
                    failureCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(failureCount.get(), 0);
    }

    @Test
    void testTaskInterruptionDuringShutdownAreUpdatedWithTheirProgressStatus() throws InterruptedException {
        int numTasks = 10;
        CountDownLatch latch = new CountDownLatch(numTasks);
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            executorService.submit(() -> {
                MoneyTransferTask task = new MockMoneyTransferTask("task" + taskId);
                transferQueue.submitTask(task);
                latch.countDown();
            });
        }

        // Allow some tasks to start processing
        Thread.sleep(50);

        // Shutdown queue while tasks are being processed
        transferQueue.shutdown();
        latch.await();
        executorService.shutdown();

        for (int i = 0; i < numTasks; i++) {
            TransferProgress progress = transferQueue.getTransferProgress("task" + i);
            assertTrue(progress.getStatus() == TransferStatus.COMPLETED || progress.getStatus() == TransferStatus.FAILED);
        }
    }

    static class MockMoneyTransferTask implements MoneyTransferTask {
        private final String id;
        private final boolean shouldFail;
        private final TransferProgress progress;

        MockMoneyTransferTask(String id) {
            this(id, false);
        }

        MockMoneyTransferTask(String id, boolean shouldFail) {
            this.id = id;
            this.shouldFail = shouldFail;
            this.progress = new TransferProgress(id, TransferStatus.INITIATED);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public TransferProgress getProgress() {
            return progress;
        }

        @Override
        public TransferResult call() {
            try {
                Thread.sleep(100); // Simulate processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (shouldFail) {
                progress.setStatus(TransferStatus.FAILED);
                return TransferResult.failure("Task failed", id, TransferResult.ErrorCode.UNKNOWN);
            } else {
                progress.setStatus(TransferStatus.COMPLETED);
                return TransferResult.success(id);
            }
        }
    }
}
