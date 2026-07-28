package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsReconcileHistRepository;
import com.ho.account.mart.core.domain.ods.audit.OdsReconcileHist;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsReconcileHistEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsReconcileHistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdsReconcileHistPersistenceAdapter implements OdsReconcileHistRepository {

    private final JpaOdsReconcileHistRepository jpaRepository;

    @Override
    public List<OdsReconcileHist> findByBaseDate(LocalDate baseDate) {
        return jpaRepository.findByBaseDate(baseDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OdsReconcileHist> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public OdsReconcileHist save(OdsReconcileHist reconcileHist) {
        return toDomain(jpaRepository.save(toEntity(reconcileHist)));
    }

    @Override
    public void saveAll(List<OdsReconcileHist> reconcileHists) {
        if (reconcileHists == null) {
            return;
        }
        jpaRepository.saveAll(reconcileHists.stream()
                .map(this::toEntity)
                .collect(Collectors.toList()));
    }

    private OdsReconcileHist toDomain(OdsReconcileHistEntity entity) {
        if (entity == null) {
            return null;
        }
        return OdsReconcileHist.builder()
                .id(entity.getId())
                .baseDate(entity.getBaseDate())
                .sourceSystem(entity.getSourceSystem())
                .targetSystem(entity.getTargetSystem())
                .reconcileItem(entity.getReconcileItem())
                .sourceAmount(entity.getSourceAmount())
                .targetAmount(entity.getTargetAmount())
                .diffAmount(entity.getDiffAmount())
                .status(entity.getStatus())
                .auditTimestamp(entity.getAuditTimestamp())
                .build();
    }

    private OdsReconcileHistEntity toEntity(OdsReconcileHist domain) {
        if (domain == null) {
            return null;
        }
        return OdsReconcileHistEntity.builder()
                .id(domain.getId())
                .baseDate(domain.getBaseDate())
                .sourceSystem(domain.getSourceSystem())
                .targetSystem(domain.getTargetSystem())
                .reconcileItem(domain.getReconcileItem())
                .sourceAmount(domain.getSourceAmount())
                .targetAmount(domain.getTargetAmount())
                .diffAmount(domain.getDiffAmount())
                .status(domain.getStatus())
                .auditTimestamp(domain.getAuditTimestamp())
                .build();
    }
}
