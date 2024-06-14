package com.neverless.service;

import com.neverless.model.TransferProgress;
import com.neverless.model.TransferResult;

import java.util.concurrent.Callable;

public interface MoneyTransferTask extends Callable<TransferResult> {
    String getId();
    TransferProgress getProgress();
}
