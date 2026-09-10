package com.ho.account.masterdata.core.infrastructure.persistence.mapper;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.FiscalPeriodEntity;
import org.springframework.stereotype.Component;

/**
 * FiscalPeriod Domain POJO <-> FiscalPeriodEntity 양방향 변환 Mapper.
 * 
 * 🐣 [Data Mapper 패턴 교육적 주석]
 * FiscalPeriod 도메인 객체와 JPA 영속성 엔티티(FiscalPeriodEntity) 간 변환 매퍼입니다.
 */
@Component
public class FiscalPeriodMapper {

    public FiscalPeriod toDomain(FiscalPeriodEntity entity) {
        if (entity == null) return null;
        // 조회/저장 결과 매핑은 명령을 재실행하지 않고 상태와 감사 이력을 그대로 복원합니다.
        return FiscalPeriod.reconstitute(
                entity.getId(), entity.getFiscalYear(), entity.getFiscalPeriod(),
                entity.getStartDate(), entity.getEndDate(), entity.getClosingStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getAuditUser());
    }

    public FiscalPeriodEntity toEntity(FiscalPeriod domain) {
        if (domain == null) return null;
        FiscalPeriodEntity entity = new FiscalPeriodEntity();
        entity.setId(domain.getId());
        entity.setFiscalYear(domain.getFiscalYear());
        entity.setFiscalPeriod(domain.getFiscalPeriod());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setClosingStatus(domain.getClosingStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }
}
