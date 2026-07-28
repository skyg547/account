package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.application.port.in.ReconciliationBatchUseCase;
import com.ho.account.reconciliation.application.port.in.RunReconciliationCommand;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReconciliationBatchService implements ReconciliationBatchUseCase {

    private final ReconciliationService reconciliationService;
    private final ReconManagerService reconManagerService;

    public ReconciliationBatchService(
            ReconciliationService reconciliationService,
            ReconManagerService reconManagerService) {
        this.reconciliationService = reconciliationService;
        this.reconManagerService = reconManagerService;
    }

    @Override
    public ReconciliationBatchRunResult runDailyReconciliation(
            LocalDate reconciliationDate,
            String runBy,
            boolean deepMode) {
        List<ReconciliationUnit> units = reconciliationService.findAllReconciliationUnits().stream()
                .filter(ReconciliationUnit::isActive)
                .toList();
        int runCount = 0;
        for (ReconciliationUnit unit : units) {
            if (deepMode) {
                reconManagerService.performDeepReconciliation(unit.getId(), reconciliationDate, runBy);
            } else {
                reconciliationService.performReconciliation(new RunReconciliationCommand(unit.getId(), reconciliationDate, runBy));
            }
            runCount++;
        }
        return new ReconciliationBatchRunResult(units.size(), runCount);
    }
}
