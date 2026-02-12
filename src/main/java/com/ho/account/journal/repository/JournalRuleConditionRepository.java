package com.ho.account.journal.repository;

import com.ho.account.journal.domain.JournalRuleCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalRuleConditionRepository extends JpaRepository<JournalRuleCondition, Long> {
    List<JournalRuleCondition> findByJournalRuleId(Long journalRuleId);
}
