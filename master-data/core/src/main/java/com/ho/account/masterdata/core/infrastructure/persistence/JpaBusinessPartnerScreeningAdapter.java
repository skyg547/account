package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.BusinessPartnerScreeningPort;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerScreeningAuditRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerScreeningRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Maps the current screening and append-only evidence history without referencing an SCD2 row. */
@Component
public class JpaBusinessPartnerScreeningAdapter implements BusinessPartnerScreeningPort {

    private final BusinessPartnerScreeningRepository currentRepository;
    private final BusinessPartnerScreeningAuditRepository auditRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaBusinessPartnerScreeningAdapter(
            BusinessPartnerScreeningRepository currentRepository,
            BusinessPartnerScreeningAuditRepository auditRepository) {
        this.currentRepository = currentRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    public Optional<BusinessPartnerScreening> findByBusinessPartnerCode(String code) {
        return currentRepository.findById(code).map(this::toDomain);
    }

    @Override
    public Optional<BusinessPartnerScreening> findLatestAt(String code, LocalDate asOfDate) {
        // A decision becomes effective when decided, while an undecided submission starts at submission.
        // The DB returns only the latest qualifying audit row, including a deterministic ID tie break.
        return auditRepository.findLatestBefore(code, asOfDate.plusDays(1).atStartOfDay(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<BusinessPartnerScreening> findByBusinessPartnerCodeForUpdate(String code) {
        return currentRepository.findByCodeForUpdate(code)
                .map(entity -> {
                    // A prior read in this transaction may be stale after waiting for the key lock.
                    entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
                    return toDomain(entity);
                });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public BusinessPartnerScreening save(BusinessPartnerScreening screening) {
        // The service first locks the permanent business-key row, including for the initial insert.
        // This row lock also guards callers that update an existing snapshot in the same transaction.
        BusinessPartnerScreeningJpaEntity entity = currentRepository
                .findByCodeForUpdate(screening.businessPartnerCode())
                .map(existing -> {
                    entityManager.refresh(existing, LockModeType.PESSIMISTIC_WRITE);
                    return existing;
                })
                .orElseGet(BusinessPartnerScreeningJpaEntity::new);
        copy(screening, entity);
        if (!entityManager.contains(entity)) {
            entityManager.persist(entity);
        }
        return toDomain(entity);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendAudit(BusinessPartnerScreening screening) {
        // Always insert a complete snapshot; resubmission never rewrites a prior decision.
        auditRepository.save(new BusinessPartnerScreeningAuditJpaEntity(toJpaEntity(screening)));
    }

    private BusinessPartnerScreeningJpaEntity toJpaEntity(BusinessPartnerScreening screening) {
        BusinessPartnerScreeningJpaEntity entity = new BusinessPartnerScreeningJpaEntity();
        copy(screening, entity);
        return entity;
    }

    private void copy(BusinessPartnerScreening source, BusinessPartnerScreeningJpaEntity target) {
        target.setBusinessPartnerCode(source.businessPartnerCode());
        target.setJurisdiction(source.jurisdiction());
        target.setIdentifierType(source.identifierType());
        target.setIdentifier(source.identifier());
        target.setEvidenceReference(source.evidenceReference());
        target.setEvidenceExpiresOn(source.evidenceExpiresOn());
        target.setStatus(source.status());
        target.setRiskRating(source.riskRating());
        target.setSubmittedBy(source.submittedBy());
        target.setSubmittedAt(source.submittedAt());
        target.setReviewedBy(source.reviewedBy());
        target.setDecisionReason(source.decisionReason());
        target.setDecidedAt(source.decidedAt());
    }

    private BusinessPartnerScreening toDomain(BusinessPartnerScreeningJpaEntity entity) {
        return new BusinessPartnerScreening(
                entity.getBusinessPartnerCode(),
                entity.getJurisdiction(),
                entity.getIdentifierType(),
                entity.getIdentifier(),
                entity.getEvidenceReference(),
                entity.getEvidenceExpiresOn(),
                entity.getStatus(),
                entity.getRiskRating(),
                entity.getSubmittedBy(),
                entity.getSubmittedAt(),
                entity.getReviewedBy(),
                entity.getDecisionReason(),
                entity.getDecidedAt());
    }

    private BusinessPartnerScreening toDomain(BusinessPartnerScreeningAuditJpaEntity entity) {
        return new BusinessPartnerScreening(
                entity.getBusinessPartnerCode(),
                entity.getJurisdiction(),
                entity.getIdentifierType(),
                entity.getIdentifier(),
                entity.getEvidenceReference(),
                entity.getEvidenceExpiresOn(),
                entity.getStatus(),
                entity.getRiskRating(),
                entity.getSubmittedBy(),
                entity.getSubmittedAt(),
                entity.getReviewedBy(),
                entity.getDecisionReason(),
                entity.getDecidedAt());
    }
}
