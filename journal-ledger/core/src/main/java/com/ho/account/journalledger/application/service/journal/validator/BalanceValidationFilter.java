package com.ho.account.journalledger.application.service.journal.validator;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * [BalanceValidationFilter]
 * 전표 불변식(Invariant) 및 차대변 합계 일치 여부를 도메인 엔티티에 위임하여 검증합니다.
 *
 * ─────────────────────────────────────────────────
 * [DDD 및 헥사고날 아키텍처 관점의 교육적 설명]
 * 이 필터는 Validation Engine의 일원으로서 외부 검증 파이프라인 흐름을 구성하지만,
 * 차대변 합계 및 필수 필드 검증 등의 비즈니스 규칙을 직접 계산하거나 내장하지 않습니다.
 * 도메인 응집도(Cohesion)를 유지하기 위해 JournalEntry Aggregate Root의 캡슐화된 
 * validateInvariants() 단일 호출을 통해 도메인 불변성 검증을 위임(Delegate)합니다.
 * ─────────────────────────────────────────────────
 */
@Component
@Order(10)
public class BalanceValidationFilter implements JournalValidationFilter {

    @Override
    public void validate(JournalEntry journalEntry) {
        // 도메인 엔티티(Aggregate Root)의 캡슐화된 종합 불변식 검증 메서드를 호출합니다.
        journalEntry.validateInvariants();
    }
}
