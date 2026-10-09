package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.JournalRuleQueryPort;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalRuleConditionRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalRuleDetailRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 자동분개 규칙 조회 포트를 Spring Data JPA 저장소에 연결하는 출력 어댑터입니다.
 */
@Component
@RequiredArgsConstructor
public class JournalRuleQueryAdapter implements JournalRuleQueryPort {

    private final JournalRuleRepository ruleRepository;
    private final JournalRuleConditionRepository conditionRepository;
    private final JournalRuleDetailRepository detailRepository;

    @Override
    public List<JournalRule> findActiveRules() {
        return ruleRepository.findByIsActiveTrueOrderByPriorityAscVersionDesc();
    }

    @Override
    public List<JournalRuleCondition> findConditions(Long journalRuleId) {
        return conditionRepository.findByJournalRuleId(journalRuleId);
    }

    @Override
    public List<JournalRuleDetail> findDetails(Long journalRuleId) {
        return detailRepository.findByJournalRuleId(journalRuleId);
    }
}
