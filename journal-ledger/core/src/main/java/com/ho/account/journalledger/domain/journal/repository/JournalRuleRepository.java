package com.ho.account.journal.repository;

import com.ho.account.journalledger.domain.journal.JournalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRuleRepository extends JpaRepository<JournalRule, Long> {
    Optional<JournalRule> findByRuleCode(String ruleCode);

    List<JournalRule> findByIsActiveTrueOrderByPriorityAscVersionDesc();

    // SCD2 湲곗??쇰줈 吏???쇱옄???쒖꽦 洹쒖튃 議고쉶
    List<JournalRule> findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(
            LocalDate date1, LocalDate date2);
}
