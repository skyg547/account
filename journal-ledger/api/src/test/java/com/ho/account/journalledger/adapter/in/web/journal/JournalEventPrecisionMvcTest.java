package com.ho.account.journalledger.adapter.in.web.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.out.JournalRuleQueryPort;
import com.ho.account.journalledger.application.port.out.SlipNumberAllocationException;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalEventPrecisionMvcTest {

    private static final LocalDate ACCOUNTING_DATE = LocalDate.of(2026, 9, 24);

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private JournalUseCase journalUseCase;

    private AtomicReference<Map<String, Object>> receivedEvent;
    private AtomicReference<JournalEntry> generatedEntry;

    @Test
    void eventAllocatorOutageReturnsServiceUnavailable() throws Exception {
        org.mockito.Mockito.doThrow(new SlipNumberAllocationException("database unavailable"))
                .when(journalUseCase).createJournalEntryFromEvent(any(), any());

        mockMvc.perform(post("/api/journals/from-event")
                        .queryParam("accountingDate", ACCOUNTING_DATE.toString())
                        .header("X-Auth-User", "journal-maker")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventData\":{}}"))
                .andExpect(status().isServiceUnavailable());
    }

    @BeforeEach
    void setUpRuleEngineAnswer() {
        objectMapper = productionObjectMapper();
        journalUseCase = org.mockito.Mockito.mock(JournalUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new JournalController(journalUseCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        JournalRuleEngine ruleEngine = precisionRuleEngine();
        receivedEvent = new AtomicReference<>();
        generatedEntry = new AtomicReference<>();

        when(journalUseCase.createJournalEntryFromEvent(any(), any())).thenAnswer(invocation -> {
            Map<String, Object> event = invocation.getArgument(0);
            LocalDate accountingDate = invocation.getArgument(1);
            receivedEvent.set(event);
            Optional<JournalEntry> result = ruleEngine.generateJournalEntry(event, accountingDate);
            result.ifPresent(generatedEntry::set);
            return result;
        });
    }

    @Test
    @DisplayName("이벤트 HTTP JSON의 큰 소수 금액을 BigDecimal 그대로 룰 엔진과 전표 라인에 전달한다")
    void preservesLargeDecimalThroughMvcAndRuleInterpolation() throws Exception {
        mockMvc.perform(post("/api/journals/from-event")
                        .queryParam("accountingDate", ACCOUNTING_DATE.toString())
                        .header("X-Auth-User", "precision-user")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventData":{"ruleCode":"PRECISION_RULE","amount":900719925474099.11,
                                  "createdBy":"payload-attacker","auditUser":"payload-attacker"}}
                                """))
                .andExpect(status().isOk());

        assertThat(objectMapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)).isTrue();
        assertExactDecimal(receivedEvent.get().get("amount"), "900719925474099.11", 2);
        assertThat(receivedEvent.get().get("createdBy")).isEqualTo("precision-user");
        assertThat(receivedEvent.get().get("auditUser")).isEqualTo("precision-user");
        assertThat(generatedEntry.get()).isNotNull();
        assertThat(generatedEntry.get().getDetails()).hasSize(2);
        assertThat(generatedEntry.get().getDetails())
                .allSatisfy(detail -> assertThat(detail.getAmount())
                        .isEqualTo(new BigDecimal("900719925474099.11")));
    }

    @Test
    @DisplayName("이벤트 HTTP JSON의 초과 소수 자릿수는 반올림 전에 도메인 경계에서 400으로 거부한다")
    void rejectsExcessFractionalPrecisionBeforeRounding() throws Exception {
        mockMvc.perform(post("/api/journals/from-event")
                        .queryParam("accountingDate", ACCOUNTING_DATE.toString())
                        .header("X-Auth-User", "precision-user")
                        .header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventData":{"ruleCode":"PRECISION_RULE","amount":100.000000000000001}}
                                """))
                .andExpect(status().isBadRequest());

        // The captured value proves the controller did not coerce or round before the real rule engine rejected it.
        assertExactDecimal(receivedEvent.get().get("amount"), "100.000000000000001", 15);
        assertThat(generatedEntry.get()).isNull();
    }

    private JournalRuleEngine precisionRuleEngine() {
        JournalRuleQueryPort queryPort = org.mockito.Mockito.mock(JournalRuleQueryPort.class);
        JournalRule rule = new JournalRule();
        rule.setId(764L);
        rule.setRuleCode("PRECISION_RULE");
        rule.setRuleName("Precision regression rule");
        rule.setActive(true);
        rule.setPriority(1);
        rule.setValidFrom(LocalDate.of(2026, 1, 1));

        when(queryPort.findActiveRules()).thenReturn(List.of(rule));
        when(queryPort.findConditions(764L)).thenReturn(List.of());
        when(queryPort.findDetails(764L)).thenReturn(List.of(
                ruleDetail(1L, "DEBIT", "11000"),
                ruleDetail(2L, "CREDIT", "21000")));
        return new JournalRuleEngine(queryPort);
    }

    private JournalRuleDetail ruleDetail(long id, String side, String accountCode) {
        JournalRuleDetail detail = new JournalRuleDetail();
        detail.setId(id);
        detail.setDrcrType(side);
        detail.setAccountSubjectCodeExpression(accountCode);
        detail.setAmountExpression("${amount}");
        return detail;
    }

    private void assertExactDecimal(Object actual, String expected, int scale) {
        assertThat(actual).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) actual).isEqualTo(new BigDecimal(expected));
        assertThat(((BigDecimal) actual).scale()).isEqualTo(scale);
    }

    private ObjectMapper productionObjectMapper() {
        AtomicReference<ObjectMapper> mapper = new AtomicReference<>();
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> mapper.set(context.getBean(ObjectMapper.class)));
        return mapper.get();
    }
}
