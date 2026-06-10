package com.ho.account.journalledger.application.port.out;

import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;

import java.util.List;

/**
 * 자동분개 규칙 조회 출력 포트.
 *
 * <p>규칙 엔진은 어떤 저장 기술을 사용하는지 모르고, 활성 규칙과 해당 규칙의
 * 조건·라인 명세를 조회한다는 업무 계약에만 의존합니다.</p>
 */
public interface JournalRuleQueryPort {

    List<JournalRule> findActiveRules();

    List<JournalRuleCondition> findConditions(Long journalRuleId);

    List<JournalRuleDetail> findDetails(Long journalRuleId);
}
