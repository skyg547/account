package com.ho.account.receivable.infrastructure.persistence.mapper;

import com.ho.account.receivable.domain.MatchingRule;
import com.ho.account.receivable.infrastructure.persistence.entity.MatchingRuleJpaEntity;
import org.springframework.stereotype.Component;

/**
 * MatchingRule Domain POJO <-> MatchingRuleJpaEntity 양방향 변환 Mapper.
 *
 * 🐣 [Hexagonal Architecture & Data Mapper 패턴 교육적 주석]
 * MatchingRule 도메인 객체와 JPA 영속성 엔티티(MatchingRuleJpaEntity) 간 양방향 데이터 매핑을 전담합니다.
 */
@Component
public class MatchingRuleMapper {

    public MatchingRule toDomain(MatchingRuleJpaEntity entity) {
        if (entity == null) return null;
        MatchingRule domain = new MatchingRule();
        domain.setId(entity.getId());
        domain.setRuleName(entity.getRuleName());
        domain.setPriority(entity.getPriority());
        domain.setMatchCriteria(entity.getMatchCriteria());
        domain.setToleranceAmount(entity.getToleranceAmount());
        domain.setActive(entity.isActive());
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    public MatchingRuleJpaEntity toEntity(MatchingRule domain) {
        if (domain == null) return null;
        MatchingRuleJpaEntity entity = new MatchingRuleJpaEntity();
        entity.setId(domain.getId());
        entity.setRuleName(domain.getRuleName());
        entity.setPriority(domain.getPriority());
        entity.setMatchCriteria(domain.getMatchCriteria());
        entity.setToleranceAmount(domain.getToleranceAmount());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
