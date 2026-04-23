package com.ho.account.journal.repository;

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

    // SCD2 湲곗???�줈 吏????�옄????�꽦 洹쒖??議고??
    List<JournalRule> findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(
            LocalDate date1, LocalDate date2);
}
