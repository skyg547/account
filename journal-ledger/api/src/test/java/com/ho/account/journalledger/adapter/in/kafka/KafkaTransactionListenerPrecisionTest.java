package com.ho.account.journalledger.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.journalledger.JournalLedgerApplication;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.out.JournalRuleQueryPort;
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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = JournalLedgerApplication.class)
@ActiveProfiles("local")
class KafkaTransactionListenerPrecisionTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecordMessageConverter recordMessageConverter;

    @Autowired
    @Qualifier("kafkaListenerContainerFactory")
    private ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory;

    @Autowired
    private ApplicationContext applicationContext;

    private JournalUseCase journalUseCase;
    private KafkaTransactionListener listener;
    private AtomicReference<Map<String, Object>> receivedEvent;
    private AtomicReference<JournalEntry> generatedEntry;

    @BeforeEach
    void setUpListenerWithRealRuleEngine() {
        JournalRuleEngine ruleEngine = precisionRuleEngine();
        journalUseCase = org.mockito.Mockito.mock(JournalUseCase.class);
        listener = new KafkaTransactionListener(journalUseCase);
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
    @DisplayName("애플리케이션 스캔은 유일한 production converter를 기본 Kafka listener factory에 연결한다")
    void wiresUniqueProductionConverterIntoDefaultListenerFactory() {
        assertThat(applicationContext.getBean(KafkaTransactionListener.class)).isNotNull();
        assertThat(applicationContext.getBeansOfType(RecordMessageConverter.class))
                .containsOnlyKeys("journalEventRecordMessageConverter")
                .containsValue(recordMessageConverter);

        Object factoryConverter = new DirectFieldAccessor(kafkaListenerContainerFactory)
                .getPropertyValue("recordMessageConverter");
        assertThat(factoryConverter).isSameAs(recordMessageConverter);
    }

    @Test
    @DisplayName("Kafka ConsumerRecord JSON의 큰 소수 금액을 production converter와 룰 엔진이 정확히 보존한다")
    void preservesLargeDecimalThroughKafkaConverterAndListener() {
        Map<String, Object> event = convert("""
                {"ruleCode":"PRECISION_RULE","amount":900719925474099.11,"accountingDate":"2026-09-24"}
                """);

        listener.listenTransactionEvent(event);

        assertThat(objectMapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)).isTrue();
        assertExactDecimal(receivedEvent.get().get("amount"), "900719925474099.11", 2);
        assertThat(receivedEvent.get().get("createdBy")).isEqualTo("service:journal-kafka-maker");
        assertThat(receivedEvent.get().get("auditUser")).isEqualTo("service:journal-kafka-maker");
        assertThat(generatedEntry.get()).isNotNull();
        assertThat(generatedEntry.get().getAccountingDate()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(generatedEntry.get().getDetails())
                .allSatisfy(detail -> assertThat(detail.getAmount())
                        .isEqualTo(new BigDecimal("900719925474099.11")));
    }

    @Test
    @DisplayName("Kafka JSON의 초과 소수 자릿수는 룰 보간 뒤 도메인 경계에서 재시도 예외로 전파한다")
    void propagatesExcessPrecisionAsListenerFailureWithoutRounding() {
        Map<String, Object> event = convert("""
                {"ruleCode":"PRECISION_RULE","amount":100.000000000000001,"accountingDate":"2026-09-24"}
                """);

        assertThatThrownBy(() -> listener.listenTransactionEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("retry/DLQ")
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .cause()
                .hasMessageContaining("소수점 2자리");

        // The listener received the original scale; failure is domain validation, not converter rounding.
        assertExactDecimal(receivedEvent.get().get("amount"), "100.000000000000001", 15);
        assertThat(generatedEntry.get()).isNull();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convert(String json) {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("transaction-events", 0, 764L, "precision-key", json);
        Message<?> message = recordMessageConverter.toMessage(record, null, null, Map.class);
        assertThat(message.getPayload()).isInstanceOf(Map.class);
        return (Map<String, Object>) message.getPayload();
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

}
