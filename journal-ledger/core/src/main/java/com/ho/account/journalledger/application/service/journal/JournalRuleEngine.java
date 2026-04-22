package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.JournalDetail;
import com.ho.account.journalledger.domain.journal.JournalEntry;
import com.ho.account.journalledger.domain.journal.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.JournalRule;
import com.ho.account.journalledger.domain.journal.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.JournalRuleDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * <h3>전표 룰 엔진 (Journal Rule Engine)</h3>
 * <p>
 * 이 엔진은 외부에서 유입된 경제적 사건(이벤트)을 분석하여, 미리 정의된 회계 처리 규칙(Rule)에 따라
 * 자동으로 복식부기 전표를 생성하는 역할을 합니다.
 * </p>
 *
 * <b>주요 기능:</b>
 * <ul>
 *   <li><b>SpEL 지원:</b> Spring Expression Language를 사용하여 "${amount} * 0.1"과 같은 동적 수식 계산을 지원합니다.</li>
 *   <li><b>조건 매칭:</b> 이벤트 데이터의 특정 필드값과 룰에 정의된 조건을 비교하여 적용 여부를 판단합니다.</li>
 *   <li><b>데이터 변환:</b> 비즈니스 데이터를 회계 데이터(계정과목, 차대변)로 변환합니다.</li>
 * </ul>
 *
 * @author 모던 재무 시스템 개발팀
 */
@Component
@RequiredArgsConstructor
public class JournalRuleEngine {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 룰과 이벤트를 결합하여 전표 엔티티를 생성합니다.
     *
     * @param rule 적용할 회계 규칙
     * @param event 외부 트랜잭션 데이터 (Map 형태)
     * @param accountingDate 전표에 기록될 회계 일자
     * @return 생성된 전표 엔티티 (DRAFT 상태)
     */
    public JournalEntry generateJournal(JournalRule rule, Map<String, Object> event, LocalDate accountingDate) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        // 룰 이름이나 설명을 표현식으로 평가하여 전표 적요로 설정
        entry.setDescription(evaluateExpression(rule.getDescription() != null ? rule.getDescription() : rule.getRuleName(), event, String.class));
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("AUTO");

        for (JournalRuleDetail ruleDetail : rule.getRuleDetails()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(ruleDetail.getDrcrType());
            
            // 금액 평가: SpEL을 통해 수식 계산 지원 (예: 부가세 10% 자동 계산)
            BigDecimal amount = evaluateExpression(ruleDetail.getAmountExpression(), event, BigDecimal.class);
            detail.setAmount(amount);
            detail.setBaseAmount(amount);
            
            // 적요 평가: 이벤트 데이터를 적요에 포함 (예: "${vendorName} 대금 지급")
            detail.setDetailDescription(evaluateExpression(ruleDetail.getDescriptionExpression(), event, String.class));
            
            entry.addDetail(detail);
        }

        return entry;
    }

    /**
     * 이벤트가 해당 룰의 적용 대상인지 조건을 검사합니다.
     *
     * @param rule 검사할 규칙
     * @param event 발생한 이벤트 데이터
     * @return 모든 조건이 일치하면 true
     */
    public boolean matches(JournalRule rule, Map<String, Object> event) {
        for (JournalRuleCondition condition : rule.getConditions()) {
            Object eventValue = event.get(condition.getField());
            if (eventValue == null) return false;

            String stringValue = String.valueOf(eventValue);
            switch (condition.getOperator()) {
                case EQUALS:
                    if (!stringValue.equals(condition.getValue())) return false;
                    break;
                case NOT_EQUALS:
                    if (stringValue.equals(condition.getValue())) return false;
                    break;
                case CONTAINS:
                    if (!stringValue.contains(condition.getValue())) return false;
                    break;
                case STARTS_WITH:
                    if (!stringValue.startsWith(condition.getValue())) return false;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /**
     * SpEL 표현식을 평가하여 실제 값을 반환합니다.
     */
    private <T> T evaluateExpression(String expression, Map<String, Object> event, Class<T> targetClass) {
        if (expression == null) return null;
        
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            // 이벤트 맵의 모든 키-값을 SpEL 변수로 등록하여 #variable 형태로 접근 가능하게 함
            context.setVariables(event);
            return parser.parseExpression(expression).getValue(context, targetClass);
        } catch (Exception e) {
            // 표현식이 단순 문자열일 경우 예외를 무시하고 원본 반환 시도
            if (targetClass == String.class) return targetClass.cast(expression);
            throw new IllegalArgumentException("표현식 평가 실패: " + expression, e);
        }
    }
}
