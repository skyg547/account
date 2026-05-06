package com.ho.account.journalledger.domain.journal.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalRuleConditionRepository extends JpaRepository<JournalRuleCondition, Long> {
    List<JournalRuleCondition> findByJournalRuleId(Long journalRuleId);
}
