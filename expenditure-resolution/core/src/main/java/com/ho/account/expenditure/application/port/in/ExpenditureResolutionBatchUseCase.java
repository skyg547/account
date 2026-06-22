package com.ho.account.expenditure.application.port.in;

import java.time.LocalDate;

public interface ExpenditureResolutionBatchUseCase {

    ApprovalBatchResult approveRequestedResolutions(LocalDate startDate, LocalDate endDate, LocalDate paymentDueDate);

    record ApprovalBatchResult(int scannedCount, int approvedCount) {
    }
}
