package com.ho.account.receivable.application.port.in;

public interface ReceivableBatchUseCase {

    AutoMatchingBatchResult runAutoMatching();

    record AutoMatchingBatchResult(int candidateCount, int attemptedCount) {
    }
}
