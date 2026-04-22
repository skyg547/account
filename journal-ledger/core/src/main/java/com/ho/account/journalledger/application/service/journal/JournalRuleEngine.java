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
 * 전표 룰 엔진 (Journal Rule Engine)
 * 외부 이벤트를 받아 정의된 룰에 따라 전표를 자동으로 생성합니다.
 * SpEL(Spring Expression Language)을 사용하여 동적인 값 계산을 지원합니다.
 */
@Component
@RequiredArgsConstructor
public class JournalRuleEngine {

    private final ExpressionParser parser = new SpelExpressionParser();

    public JournalEntry generateJournal(JournalRule rule, Map<String, Object> event, LocalDate accountingDate) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(evaluateExpression(rule.getDescription() != null ? rule.getDescription() : rule.getRuleName(), event, String.class));
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("AUTO");

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(event);

        for (JournalRuleDetail ruleDetail : rule.getRuleDetails()) {
            JournalDetail detail = new JournalDetail();
            detail.setDrcrType(ruleDetail.getDrcrType());
            
            // 계정과목 코드 평가
            String accountCode = evaluateExpression(ruleDetail.getAccountSubjectCodeExpression(), event, String.class);
            // 주의: 실제 엔티티 매핑은 Service 계층에서 수행하도록 유도하거나 여기서 Code만 채움
            // 여기서는 DTO 성격으로 Code만 먼저 채우는 방식으로 설계 (후처리는 JournalService)
            
            // 금액 평가 (수식 지원: e.g. #amount * 0.1)
            BigDecimal amount = evaluateExpression(ruleDetail.getAmountExpression(), event, BigDecimal.class);
            
            detail.setAmount(amount);
            detail.setDetailDescription(evaluateExpression(ruleDetail.getDescriptionExpression(), event, String.class));
            
            // 임시로 코드만 저장하기 위한 필드가 JournalDetail에 있는지 확인 필요
            // 만약 없다면 JournalDetail 구조를 확장하거나 Service에서 매핑 로직 추가
            
            entry.addDetail(detail);
        }

        return entry;
    }

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
                case ENDS_WITH:
                    if (!stringValue.endsWith(condition.getValue())) return false;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private <T> T evaluateExpression(String expression, Map<String, Object> event, Class<T> targetClass) {
        if (expression == null) return null;
        if (!expression.contains("#") && !expression.contains("+") && !expression.contains("-") && !expression.contains("*")) {
            // 단순 문자열일 경우 SpEL 없이 처리 시도 (성능 최적화)
            if (targetClass == String.class) return targetClass.cast(expression);
        }
        
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariables(event);
            return parser.parseExpression(expression).getValue(context, targetClass);
        } catch (Exception e) {
            // 표현식 평가 실패 시 원본 문자열 반환 시도 (문자열일 경우)
            if (targetClass == String.class) return targetClass.cast(expression);
            throw new IllegalArgumentException("Failed to evaluate expression: " + expression, e);
        }
    }
}
