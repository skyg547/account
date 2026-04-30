package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journal.repository.JournalRuleConditionRepository;
import com.ho.account.journal.repository.JournalRuleDetailRepository;
import com.ho.account.journal.repository.JournalRuleRepository;
import com.ho.account.journalledger.domain.journal.domain.ConditionOperator;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleCondition;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalRuleEngineTest {

    @Mock
    private JournalRuleRepository journalRuleRepository;
    @Mock
    private JournalRuleConditionRepository journalRuleConditionRepository;
    @Mock
    private JournalRuleDetailRepository journalRuleDetailRepository;
    @Mock
    private AccountSubjectPersistencePort accountSubjectPersistencePort;
    @Mock
    private DepartmentPersistencePort departmentPersistencePort;
    @Mock
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;
    @Mock
    private CurrencyPersistencePort currencyPersistencePort;

    private JournalRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new JournalRuleEngine(
                journalRuleRepository,
                journalRuleConditionRepository,
                journalRuleDetailRepository,
                accountSubjectPersistencePort,
                departmentPersistencePort,
                businessPartnerPersistencePort,
                currencyPersistencePort);
    }

    @Test
    @DisplayName("규칙과 조건이 일치하면 JournalEntry를 생성한다.")
    void generateJournalEntryWhenRuleMatches() {
        JournalRule rule = createRule(1L, "ASSET_ACQUISITION");

        JournalRuleCondition typeCondition = new JournalRuleCondition();
        typeCondition.setField("transactionType");
        typeCondition.setOperator(ConditionOperator.EQUALS);
        typeCondition.setValue("ASSET_ACQUISITION");

        JournalRuleCondition amountCondition = new JournalRuleCondition();
        amountCondition.setField("amount");
        amountCondition.setOperator(ConditionOperator.GREATER_THAN_OR_EQUAL);
        amountCondition.setValue("1");

        JournalRuleDetail debit = new JournalRuleDetail();
        debit.setId(1L);
        debit.setDrcrType("DEBIT");
        debit.setAccountSubjectCodeExpression("15000");
        debit.setAmountExpression("${amount}");
        debit.setDescriptionExpression("'취득-' + #assetCode");

        JournalRuleDetail credit = new JournalRuleDetail();
        credit.setId(2L);
        credit.setDrcrType("CREDIT");
        credit.setAccountSubjectCodeExpression("10100");
        credit.setAmountExpression("${amount}");
        credit.setDescriptionExpression("'현금지급'");

        when(journalRuleRepository.findByIsActiveTrueOrderByPriorityAscVersionDesc()).thenReturn(List.of(rule));
        when(journalRuleConditionRepository.findByJournalRuleId(1L)).thenReturn(List.of(typeCondition, amountCondition));
        when(journalRuleDetailRepository.findByJournalRuleId(1L)).thenReturn(List.of(debit, credit));
        when(accountSubjectPersistencePort.findByCode("15000")).thenReturn(Optional.of(account("15000")));
        when(accountSubjectPersistencePort.findByCode("10100")).thenReturn(Optional.of(account("10100")));
        when(currencyPersistencePort.findByCode("KRW")).thenReturn(Optional.empty());

        Optional<JournalEntry> generated = engine.generateJournalEntry(
                Map.of(
                        "transactionType", "ASSET_ACQUISITION",
                        "amount", new BigDecimal("120000"),
                        "assetCode", "FA-0001"),
                LocalDate.of(2026, 4, 30));

        assertThat(generated).isPresent();
        JournalEntry entry = generated.orElseThrow();
        assertThat(entry.getDetails()).hasSize(2);
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("120000");
        assertThat(entry.getDetails().get(0).getDetailDescription()).isEqualTo("취득-FA-0001");
        assertThat(entry.getDetails().get(1).getAmount()).isEqualByComparingTo("120000");
    }

    @Test
    @DisplayName("조건이 맞지 않으면 빈 Optional을 반환한다.")
    void returnEmptyWhenConditionsNotMatched() {
        JournalRule rule = createRule(2L, "ASSET_DEPRECIATION");

        JournalRuleCondition typeCondition = new JournalRuleCondition();
        typeCondition.setField("transactionType");
        typeCondition.setOperator(ConditionOperator.EQUALS);
        typeCondition.setValue("ASSET_DEPRECIATION");

        when(journalRuleRepository.findByIsActiveTrueOrderByPriorityAscVersionDesc()).thenReturn(List.of(rule));
        when(journalRuleConditionRepository.findByJournalRuleId(2L)).thenReturn(List.of(typeCondition));

        Optional<JournalEntry> generated = engine.generateJournalEntry(
                Map.of("transactionType", "ASSET_ACQUISITION", "amount", new BigDecimal("30000")),
                LocalDate.of(2026, 4, 30));

        assertThat(generated).isEmpty();
    }

    @Test
    @DisplayName("금액 수식 표현식(사칙연산)을 평가한다.")
    void evaluatesAmountExpression() {
        JournalRule rule = createRule(3L, "VAT_RULE");

        JournalRuleDetail debit = new JournalRuleDetail();
        debit.setId(10L);
        debit.setDrcrType("DEBIT");
        debit.setAccountSubjectCodeExpression("13500");
        debit.setAmountExpression("${transaction.totalAmount} / 11");

        JournalRuleDetail credit = new JournalRuleDetail();
        credit.setId(11L);
        credit.setDrcrType("CREDIT");
        credit.setAccountSubjectCodeExpression("21100");
        credit.setAmountExpression("${transaction.totalAmount} / 11");

        when(journalRuleRepository.findByIsActiveTrueOrderByPriorityAscVersionDesc()).thenReturn(List.of(rule));
        when(journalRuleConditionRepository.findByJournalRuleId(3L)).thenReturn(List.of());
        when(journalRuleDetailRepository.findByJournalRuleId(3L)).thenReturn(List.of(debit, credit));
        when(accountSubjectPersistencePort.findByCode("13500")).thenReturn(Optional.of(account("13500")));
        when(accountSubjectPersistencePort.findByCode("21100")).thenReturn(Optional.of(account("21100")));
        when(currencyPersistencePort.findByCode("KRW")).thenReturn(Optional.empty());

        Optional<JournalEntry> generated = engine.generateJournalEntry(
                Map.of("ruleCode", "VAT_RULE", "transaction", Map.of("totalAmount", new BigDecimal("110000"))),
                LocalDate.of(2026, 4, 30));

        assertThat(generated).isPresent();
        JournalEntry entry = generated.orElseThrow();
        assertThat(entry.getDetails()).hasSize(2);
        assertThat(entry.getDetails().get(0).getAmount()).isEqualByComparingTo("10000");
        assertThat(entry.getDetails().get(1).getAmount()).isEqualByComparingTo("10000");
    }

    private JournalRule createRule(Long id, String ruleCode) {
        JournalRule rule = new JournalRule();
        rule.setId(id);
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleCode + "_NAME");
        rule.setActive(true);
        rule.setPriority(1);
        rule.setValidFrom(LocalDate.of(2025, 1, 1));
        return rule;
    }

    private AccountSubject account(String code) {
        AccountSubject accountSubject = new AccountSubject();
        accountSubject.setCode(code);
        accountSubject.setName("TEST-" + code);
        return accountSubject;
    }
}
