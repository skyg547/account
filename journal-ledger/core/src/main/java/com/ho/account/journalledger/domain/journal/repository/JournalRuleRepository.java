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

    // SCD2 疫꿸퀣???곗쨮 筌왖????깆쁽????뽮쉐 域뱀뮇??鈺곌퀬??
    List<JournalRule> findByIsActiveTrueAndValidFromBeforeAndValidToAfterOrValidToIsNullOrderByPriorityAscVersionDesc(
            LocalDate date1, LocalDate date2);
}
