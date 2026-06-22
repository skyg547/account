package com.ho.account.expenditure.application.service;

import com.ho.account.expenditure.application.port.in.ExpenditureResolutionBatchUseCase;
import com.ho.account.expenditure.application.port.in.ExpenditureResolutionUseCase;
import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.ExpenditureResolutionStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenditureResolutionBatchService implements ExpenditureResolutionBatchUseCase {

    private final ExpenditureResolutionPersistencePort resolutionPersistencePort;
    private final ExpenditureResolutionUseCase expenditureResolutionUseCase;

    public ExpenditureResolutionBatchService(
            ExpenditureResolutionPersistencePort resolutionPersistencePort,
            ExpenditureResolutionUseCase expenditureResolutionUseCase) {
        this.resolutionPersistencePort = resolutionPersistencePort;
        this.expenditureResolutionUseCase = expenditureResolutionUseCase;
    }

    @Override
    public ApprovalBatchResult approveRequestedResolutions(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate paymentDueDate) {
        List<ExpenditureResolution> resolutions =
                resolutionPersistencePort.findByResolutionDateBetween(startDate, endDate);
        int approved = 0;
        for (ExpenditureResolution resolution : resolutions) {
            if (resolution.getId() == null
                    || resolution.getStatus() != ExpenditureResolutionStatus.REQUESTED
                    || resolution.getPaymentDate().isAfter(paymentDueDate)) {
                continue;
            }
            expenditureResolutionUseCase.approveResolution(resolution.getId());
            approved++;
        }
        return new ApprovalBatchResult(resolutions.size(), approved);
    }
}
