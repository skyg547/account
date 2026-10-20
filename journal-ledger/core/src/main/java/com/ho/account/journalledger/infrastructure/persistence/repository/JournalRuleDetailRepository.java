package com.ho.account.journalledger.infrastructure.persistence.repository;

import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalRuleDetailRepository extends JpaRepository<JournalRuleDetail, Long> {
    List<JournalRuleDetail> findByJournalRuleId(Long journalRuleId);
}
