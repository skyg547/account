package com.ho.account.journal.repository;

import com.ho.account.journalledger.domain.journal.JournalRuleDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalRuleDetailRepository extends JpaRepository<JournalRuleDetail, Long> {
    List<JournalRuleDetail> findByJournalRuleId(Long journalRuleId);
}
