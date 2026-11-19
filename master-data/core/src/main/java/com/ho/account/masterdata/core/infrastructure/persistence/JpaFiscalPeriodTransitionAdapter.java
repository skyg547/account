package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.FiscalPeriodTransitionPersistencePort;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionConflictException;
import com.ho.account.masterdata.core.domain.exception.FiscalPeriodTransitionNotFoundException;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.domain.model.FiscalPeriodTransition;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.FiscalPeriodTransitionEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodTransitionRepository;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Persistence mechanics only; domain receipts decide which lifecycle changes are legal. */
@Repository
public class JpaFiscalPeriodTransitionAdapter implements FiscalPeriodTransitionPersistencePort {
    private final FiscalPeriodTransitionRepository repository;

    public JpaFiscalPeriodTransitionAdapter(FiscalPeriodTransitionRepository repository) {
        this.repository = repository;
    }

    @Override public Optional<FiscalPeriodTransition> findByOperationId(String id) {
        return repository.findById(id).map(this::toDomain);
    }
    @Override public Optional<Long> findPeriodId(String id) { return repository.findPeriodIdByOperationId(id); }
    @Override public boolean existsByFiscalPeriodId(Long id) { return repository.existsByFiscalPeriodId(id); }
    @Override public boolean hasPrepared(Long id) {
        return repository.findFirstByFiscalPeriodIdAndPhase(id, FiscalPeriodTransitionEntity.Phase.PREPARED).isPresent();
    }
    @Override public boolean hasUnconfirmedReopen(Long id) {
        return repository.findFirstByFiscalPeriodIdAndPhaseAndTargetStatus(
                id, FiscalPeriodTransitionEntity.Phase.APPLIED, "OPEN").isPresent();
    }

    @Override
    public FiscalPeriodTransition insert(FiscalPeriodTransition receipt) {
        FiscalPeriodTransitionEntity entity = new FiscalPeriodTransitionEntity();
        entity.setOperationId(receipt.operationId());
        entity.setFiscalPeriodId(receipt.fiscalPeriodId());
        entity.setExpectedStatus(receipt.expectedStatus().name());
        entity.setTargetStatus(receipt.targetStatus().name());
        entity.setAuditUser(receipt.auditUser());
        entity.setCreatedAt(receipt.createdAt());
        copyLifecycle(receipt, entity);
        try {
            return toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException failure) {
            // A different period may reserve the ID after our missing read. INSERT never merges it.
            // PostgreSQL aborts the losing transaction: propagate conflict without querying/retrying.
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                    throw new FiscalPeriodTransitionConflictException("Fiscal transition identity is already reserved");
                }
            }
            throw failure;
        }
    }

    @Override
    public void saveLifecycle(FiscalPeriodTransition receipt) {
        FiscalPeriodTransitionEntity entity = repository.findById(receipt.operationId())
                .orElseThrow(FiscalPeriodTransitionNotFoundException::new);
        copyLifecycle(receipt, entity);
        repository.save(entity);
    }

    @Override public Optional<Admission> findAdmissionById(Long id) {
        return repository.findAdmissionById(id).map(this::admission);
    }
    @Override public Optional<Admission> findAdmissionByPeriodKey(String year, String period) {
        return repository.findAdmissionByPeriodKey(year, period).map(this::admission);
    }

    private Admission admission(FiscalPeriodTransitionRepository.AdmissionView view) {
        return new Admission(view.getId(), view.getFiscalYear(), view.getFiscalPeriod(),
                view.getStartDate(), view.getEndDate(), view.getClosingStatus());
    }

    private void copyLifecycle(FiscalPeriodTransition receipt, FiscalPeriodTransitionEntity entity) {
        entity.setPhase(FiscalPeriodTransitionEntity.Phase.valueOf(receipt.phase().name()));
        entity.setAppliedAt(receipt.appliedAt());
        entity.setCancelledAt(receipt.cancelledAt());
        entity.setConfirmedAt(receipt.confirmedAt());
    }

    private FiscalPeriodTransition toDomain(FiscalPeriodTransitionEntity entity) {
        return new FiscalPeriodTransition(entity.getOperationId(), entity.getFiscalPeriodId(),
                FiscalPeriod.ClosingStatus.valueOf(entity.getExpectedStatus()),
                FiscalPeriod.ClosingStatus.valueOf(entity.getTargetStatus()), entity.getAuditUser(),
                FiscalPeriodTransition.Phase.valueOf(entity.getPhase().name()), entity.getCreatedAt(),
                entity.getAppliedAt(), entity.getCancelledAt(), entity.getConfirmedAt());
    }
}
