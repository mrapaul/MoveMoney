package com.neverless.service;

import com.neverless.model.TransferProgress;
import com.neverless.model.TransferResult;
import com.neverless.model.TransferStatus;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TransferQueue {
    private final ManyToOneConcurrentArrayQueue<FutureTask<TransferResult>> taskQueue;
    private final Thread workerThread;
    private final AtomicBoolean running;
    private final Map<String, TransferProgress> progressMap;

    public TransferQueue(int capacity) {
        this.taskQueue = new ManyToOneConcurrentArrayQueue<>(capacity);
        this.running = new AtomicBoolean(true);
        this.progressMap = new ConcurrentHashMap<>();

        this.workerThread = new Thread(() -> {
            while (running.get() || !taskQueue.isEmpty()) {
                try {
                    FutureTask<TransferResult> task = taskQueue.poll();
                    if (task != null) {
                        task.run();
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        this.workerThread.start();
    }

    public TransferResult submitTask(MoneyTransferTask task) {
        String taskId = task.getId();
        if (running.get()) {
            FutureTask<TransferResult> futureTask = new FutureTask<>(task);
            progressMap.put(taskId, task.getProgress());
            taskQueue.offer(futureTask);
            try {
                return futureTask.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                return TransferResult.failure("Task execution interrupted", taskId, TransferResult.ErrorCode.UNKNOWN);
            }
        }
        return TransferResult.failure("Queue is not running", taskId, TransferResult.ErrorCode.UNKNOWN);
    }

    public TransferProgress getTransferProgress(String transferId) {
        return progressMap.getOrDefault(transferId, new TransferProgress(transferId, TransferStatus.UNKNOWN));
    }

    public void updateProgress(String transferId, TransferStatus status) {
        TransferProgress progress = progressMap.get(transferId);
        if (progress != null) {
            progress.setStatus(status);
        }
    }

    public void shutdown() {
        running.set(false);
        workerThread.interrupt();
        try {
            workerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
