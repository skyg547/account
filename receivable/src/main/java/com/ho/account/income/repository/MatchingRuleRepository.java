package com.ho.account.income.repository;

import com.ho.account.income.domain.MatchingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchingRuleRepository extends JpaRepository<MatchingRule, Long> {
    List<MatchingRule> findByIsActiveOrderByPriorityAsc(boolean isActive);
}
