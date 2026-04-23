package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.domain.journal.domain.ConditionOperator;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JournalRuleEngineTest {

    private final JournalRuleEngine engine = new JournalRuleEngine();

    @Test
    @DisplayName("SpEL ?œí˜„?ì„ ?¬ìš©?˜ì—¬ ê¸ˆì•¡???™ì ?¼ë¡œ ê³„ì‚°?????ˆë‹¤.")
    void generatesJournalWithSpelExpressions() {
        // Given: ?ì‹œë¹?30,000???´ë²¤??
        Map<String, Object> event = Map.of(
                "amount", new BigDecimal("30000"),
                "vendorName", "ì¹´ì¹´?¤íƒ??
        );

        JournalRule rule = new JournalRule();
        rule.setRuleName("?¬ë¹„êµí†µë¹??ë™ë¶„ê°œ");
        
        // ì°¨ë?: ?¬ë¹„êµí†µë¹?30,000??
        JournalRuleDetail debitDetail = new JournalRuleDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAmountExpression("#amount"); // SpEL ë³€???¬ìš©
        debitDetail.setDescriptionExpression("#vendorName + ' ?´ìš©ê±?");
        rule.addRuleDetail(debitDetail);

        // When
        JournalEntry entry = engine.generateJournal(rule, event, LocalDate.now());

        // Then
        assertThat(entry.getDetails()).hasSize(1);
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("30000");
        assertThat(entry.getDetails().get(0).getDetailDescription()).isEqualTo("ì¹´ì¹´?¤íƒ???´ìš©ê±?);
    }

    @Test
    @DisplayName("?´ë²¤???°ì´?°ê? ë£°ì˜ ì¡°ê±´??ë§Œì¡±?˜ëŠ”ì§€ ?ë‹¨?????ˆë‹¤.")
    void matchesRuleConditions() {
        // Given: ?ë§¤(SALE) ?¸ëœ??…˜ ?´ë²¤??
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
    @DisplayName("SpEL???œìš©?˜ì—¬ ë¶€ê°€??10%)ë¥??ë™?¼ë¡œ ê³„ì‚°?????ˆë‹¤.")
    void calculatesVatAutomatically() {
        // Given: ë¬¼í’ˆ êµ¬ë§¤ 100,000??
        Map<String, Object> event = Map.of("totalAmount", new BigDecimal("110000"));

        JournalRule rule = new JournalRule();
        
        // ê³µê¸‰ê°€??(110,000 / 1.1 = 100,000)
        JournalRuleDetail supplyDetail = new JournalRuleDetail();
        supplyDetail.setAmountExpression("#totalAmount.divide(new java.math.BigDecimal('1.1'), 0, java.math.RoundingMode.HALF_UP)");
        rule.addRuleDetail(supplyDetail);

        // When
        JournalEntry entry = engine.generateJournal(rule, event, LocalDate.now());

        // Then
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("100000");
    }
}
