package com.ho.account.expenditure.infrastructure.persistence.mapper;

import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.infrastructure.persistence.entity.PaymentRunJpaEntity;
import org.springframework.stereotype.Component;

/**
 * PaymentRun Domain POJO <-> PaymentRunJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * 도메인 계층(PaymentRun)의 프레임워크 독립성을 유지하기 위해 영속성 계층의 JPA Entity와 매핑을 수행합니다.
 */
@Component
public class PaymentRunMapper {

    public PaymentRun toDomain(PaymentRunJpaEntity entity) {
        if (entity == null) return null;
        PaymentRun domain = new PaymentRun();
        domain.setId(entity.getId());
        domain.setRunDate(entity.getRunDate());
        domain.setDescription(entity.getDescription());
        domain.setStatus(entity.getStatus());
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public PaymentRunJpaEntity toEntity(PaymentRun domain) {
        if (domain == null) return null;
        PaymentRunJpaEntity entity = new PaymentRunJpaEntity();
        entity.setId(domain.getId());
        entity.setRunDate(domain.getRunDate());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
