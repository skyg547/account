package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.ConditionOperator;
import com.ho.account.journalledger.domain.journal.JournalEntry;
import com.ho.account.journalledger.domain.journal.JournalRule;
import com.ho.account.journalledger.domain.journal.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.JournalRuleDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JournalRuleEngineTest {

    private final JournalRuleEngine engine = new JournalRuleEngine();

    @Test
    @DisplayName("SpEL 표현식을 사용하여 금액을 동적으로 계산할 수 있다.")
    void generatesJournalWithSpelExpressions() {
        // Given: 택시비 30,000원 이벤트
        Map<String, Object> event = Map.of(
                "amount", new BigDecimal("30000"),
                "vendorName", "카카오택시"
        );

        JournalRule rule = new JournalRule();
        rule.setRuleName("여비교통비 자동분개");
        
        // 차변: 여비교통비 30,000원
        JournalRuleDetail debitDetail = new JournalRuleDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAmountExpression("#amount"); // SpEL 변수 사용
        debitDetail.setDescriptionExpression("#vendorName + ' 이용건'");
        rule.addRuleDetail(debitDetail);

        // When
        JournalEntry entry = engine.generateJournal(rule, event, LocalDate.now());

        // Then
        assertThat(entry.getDetails()).hasSize(1);
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("30000");
        assertThat(entry.getDetails().get(0).getDetailDescription()).isEqualTo("카카오택시 이용건");
    }

    @Test
    @DisplayName("이벤트 데이터가 룰의 조건을 만족하는지 판단할 수 있다.")
    void matchesRuleConditions() {
        // Given: 판매(SALE) 트랜잭션 이벤트
        Map<String, Object> event = Map.of("trxType", "SALE");

        JournalRule rule = new JournalRule();
        JournalRuleCondition condition = new JournalRuleCondition();
        condition.setField("trxType");
        condition.setOperator(ConditionOperator.EQUALS);
        condition.setValue("SALE");
        rule.addCondition(condition);

        // When
        boolean isMatch = engine.matches(rule, event);

        // Then
        assertThat(isMatch).isTrue();
    }

    @Test
    @DisplayName("SpEL을 활용하여 부가세(10%)를 자동으로 계산할 수 있다.")
    void calculatesVatAutomatically() {
        // Given: 물품 구매 100,000원
        Map<String, Object> event = Map.of("totalAmount", new BigDecimal("110000"));

        JournalRule rule = new JournalRule();
        
        // 공급가액 (110,000 / 1.1 = 100,000)
        JournalRuleDetail supplyDetail = new JournalRuleDetail();
        supplyDetail.setAmountExpression("#totalAmount.divide(new java.math.BigDecimal('1.1'), 0, java.math.RoundingMode.HALF_UP)");
        rule.addRuleDetail(supplyDetail);

        // When
        JournalEntry entry = engine.generateJournal(rule, event, LocalDate.now());

        // Then
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("100000");
    }
}
