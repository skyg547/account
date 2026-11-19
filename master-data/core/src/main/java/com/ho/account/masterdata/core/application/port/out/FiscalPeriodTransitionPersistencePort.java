package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.FiscalPeriodTransition;
import java.time.LocalDate;
import java.util.Optional;

/** Durable receipt operations used while the application holds the corresponding period lock. */
public interface FiscalPeriodTransitionPersistencePort {
    record Admission(Long id, String fiscalYear, String fiscalPeriod, LocalDate startDate,
            LocalDate endDate, String closingStatus) {}

    Optional<FiscalPeriodTransition> findByOperationId(String operationId);
    Optional<Long> findPeriodId(String operationId);
    boolean existsByFiscalPeriodId(Long fiscalPeriodId);
    boolean hasPrepared(Long fiscalPeriodId);
    boolean hasUnconfirmedReopen(Long fiscalPeriodId);
    FiscalPeriodTransition insert(FiscalPeriodTransition receipt);
    void saveLifecycle(FiscalPeriodTransition receipt);
    Optional<Admission> findAdmissionById(Long id);
    Optional<Admission> findAdmissionByPeriodKey(String fiscalYear, String fiscalPeriod);
}
