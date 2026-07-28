package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.OdsDqAuditRepository;
import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsDqAuditEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsDqAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdsDqAuditPersistenceAdapter implements OdsDqAuditRepository {

    private final JpaOdsDqAuditRepository jpaRepository;

    @Override
    public List<OdsDqAudit> findByBaseDate(LocalDate baseDate) {
        return jpaRepository.findByBaseDate(baseDate).stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public List<OdsDqAudit> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::main)
                .collect(Collectors.toList());
    }

    @Override
    public OdsDqAudit save(OdsDqAudit domain) {
        return main(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public void saveAll(List<OdsDqAudit> domains) {
        if (domains == null) return;
        List<OdsDqAuditEntity> entities = domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    private OdsDqAudit main(OdsDqAuditEntity entity) {
        if (entity == null) return null;
        return OdsDqAudit.builder()
                .id(entity.getId())
                .baseDate(entity.getBaseDate())
                .accountNo(entity.getAccountNo())
                .auditType(entity.getAuditType())
                .auditMessage(entity.getAuditMessage())
                .severity(entity.getSeverity())
                .auditTimestamp(entity.getAuditTimestamp())
                .build();
    }

    private OdsDqAuditEntity toEntity(OdsDqAudit domain) {
        if (domain == null) return null;
        return OdsDqAuditEntity.builder()
                .id(domain.getId())
                .baseDate(domain.getBaseDate())
                .accountNo(domain.getAccountNo())
                .auditType(domain.getAuditType())
                .auditMessage(domain.getAuditMessage())
                .severity(domain.getSeverity())
                .auditTimestamp(domain.getAuditTimestamp())
                .build();
    }
}
