package com.ho.account.journal.repository;

import com.ho.account.journal.domain.JournalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRuleRepository extends JpaRepository<JournalRule, Long> {
    Optional<JournalRule> findByRuleCode(String ruleCode);

    List<JournalRule> findByIsActiveTrueOrderByPriorityAscVersionDesc();

    // For SCD2, find active rules for a given date
    List<JournalRule> findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(
            LocalDate date1, LocalDate date2);
}
