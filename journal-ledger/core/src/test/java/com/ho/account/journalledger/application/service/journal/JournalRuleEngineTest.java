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
    @DisplayName("SpEL ?쒗쁽?앹쓣 ?ъ슜?섏뿬 湲덉븸???숈쟻?쇰줈 怨꾩궛?????덈떎.")
    void generatesJournalWithSpelExpressions() {
        // Given: ?앹떆鍮?30,000???대깽??
        Map<String, Object> event = Map.of(
                "amount", new BigDecimal("30000"),
                "vendorName", "移댁뭅?ㅽ깮??
        );

        JournalRule rule = new JournalRule();
        rule.setRuleName("?щ퉬援먰넻鍮??먮룞遺꾧컻");
        
        // 李⑤?: ?щ퉬援먰넻鍮?30,000??
        JournalRuleDetail debitDetail = new JournalRuleDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAmountExpression("#amount"); // SpEL 蹂???ъ슜
        debitDetail.setDescriptionExpression("#vendorName + ' ?댁슜嫄?");
        rule.addRuleDetail(debitDetail);

        // When
        JournalEntry entry = engine.generateJournal(rule, event, LocalDate.now());

        // Then
        assertThat(entry.getDetails()).hasSize(1);
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("30000");
        assertThat(entry.getDetails().get(0).getDetailDescription()).isEqualTo("移댁뭅?ㅽ깮???댁슜嫄?);
    }

    @Test
    @DisplayName("?대깽???곗씠?곌? 猷곗쓽 議곌굔??留뚯”?섎뒗吏 ?먮떒?????덈떎.")
    void matchesRuleConditions() {
        // Given: ?먮ℓ(SALE) ?몃옖??뀡 ?대깽??
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
    @DisplayName("SpEL???쒖슜?섏뿬 遺媛??10%)瑜??먮룞?쇰줈 怨꾩궛?????덈떎.")
    void calculatesVatAutomatically() {
        // Given: 臾쇳뭹 援щℓ 100,000??
        Map<String, Object> event = Map.of("totalAmount", new BigDecimal("110000"));

        JournalRule rule = new JournalRule();
        
        // 怨듦툒媛??(110,000 / 1.1 = 100,000)
        JournalRuleDetail supplyDetail = new JournalRuleDetail();
        supplyDetail.setAmountExpression("#totalAmount.divide(new java.math.BigDecimal('1.1'), 0, java.math.RoundingMode.HALF_UP)");
        rule.addRuleDetail(supplyDetail);

        // When
        JournalEntry entry = engine.generateJournal(rule, event, LocalDate.now());

        // Then
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("100000");
    }
}
