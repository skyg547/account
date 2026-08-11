package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.AccountSubjectEntity;
import org.springframework.stereotype.Component;

/**
 * AccountSubject Domain POJO <-> AccountSubjectEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * Data Mapper는 Domain POJO와 JPA Entity 사이의 변환 책임을 전담합니다.
 * 이 매퍼를 두어 Domain POJO는 JPA의 엔티티 구조 및 생명주기를 알지 못하고,
 * JPA Entity는 도메인 비즈니스 규칙을 포함하지 않도록 완벽히 양방향 캡슐화를 달성합니다.
 */
@Component
public class AccountSubjectMapper {

    public AccountSubject toDomain(AccountSubjectEntity entity) {
        if (entity == null) return null;
        AccountSubject domain = new AccountSubject();
        domain.setId(entity.getId());
        domain.setCode(entity.getCode());
        domain.setName(entity.getName());
        domain.setCategory(entity.getCategory());
        domain.setAccountType(entity.getAccountType());
        domain.setBalanceType(entity.getBalanceType());
        domain.setReportLine(entity.getReportLine());
        domain.setRegulatoryMappingCode(entity.getRegulatoryMappingCode());
        domain.setUnsettled(entity.isUnsettled());
        domain.setValidFrom(entity.getValidFrom());
        domain.setValidTo(entity.getValidTo());
        domain.setFixedAsset(entity.isFixedAsset());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());

        if (entity.getParent() != null) {
            domain.setParent(toDomain(entity.getParent()));
        }
        return domain;
    }

    public AccountSubjectEntity toEntity(AccountSubject domain) {
        if (domain == null) return null;
        AccountSubjectEntity entity = new AccountSubjectEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setCategory(domain.getCategory());
        entity.setAccountType(domain.getAccountType());
        entity.setBalanceType(domain.getBalanceType());
        entity.setReportLine(domain.getReportLine());
        entity.setRegulatoryMappingCode(domain.getRegulatoryMappingCode());
        entity.setUnsettled(domain.isUnsettled());
        entity.setValidFrom(domain.getValidFrom());
        entity.setValidTo(domain.getValidTo());
        entity.setFixedAsset(domain.isFixedAsset());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());

        if (domain.getParent() != null) {
            entity.setParent(toEntity(domain.getParent()));
        }
        return entity;
    }
}
