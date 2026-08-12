package com.ho.account.receivable.repository;

import com.ho.account.receivable.infrastructure.persistence.entity.MatchingRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchingRuleRepository extends JpaRepository<MatchingRuleJpaEntity, Long> {
    List<MatchingRuleJpaEntity> findByIsActiveOrderByPriorityAsc(boolean isActive);
}
