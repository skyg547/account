package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.domain.ReconciliationType;
import java.time.LocalDate;

public class ReconciliationRequestDto {

    private LocalDate reconciliationDate;

    private ReconciliationType reconciliationType;

    private Long closingPeriodId;

    private String runBy;

    // Getter 및 Setter
    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public ReconciliationType getReconciliationType() {
        return reconciliationType;
    }

    public void setReconciliationType(ReconciliationType reconciliationType) {
        this.reconciliationType = reconciliationType;
    }

    public Long getClosingPeriodId() {
        return closingPeriodId;
    }

    public void setClosingPeriodId(Long closingPeriodId) {
        this.closingPeriodId = closingPeriodId;
    }

    public String getRunBy() {
        return runBy;
    }

    public void setRunBy(String runBy) {
        this.runBy = runBy;
    }
}
