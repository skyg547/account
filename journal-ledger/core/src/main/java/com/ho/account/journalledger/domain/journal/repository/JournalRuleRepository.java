package com.ho.account.journalledger.domain.journal.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRuleRepository extends JpaRepository<JournalRule, Long> {
    Optional<JournalRule> findByRuleCode(String ruleCode);

    List<JournalRule> findByIsActiveTrueOrderByPriorityAscVersionDesc();

    /**
     * 특정 시점에 활성화된 규칙을 조회합니다.
     */
    default List<JournalRule> findActiveRulesAt(LocalDate date) {
        return findByIsActiveTrueOrderByPriorityAscVersionDesc().stream()
                .filter(rule -> (rule.getValidFrom() == null || !date.isBefore(rule.getValidFrom())) &&
                                (rule.getValidTo() == null || !date.isAfter(rule.getValidTo())))
                .toList();
    }
}
