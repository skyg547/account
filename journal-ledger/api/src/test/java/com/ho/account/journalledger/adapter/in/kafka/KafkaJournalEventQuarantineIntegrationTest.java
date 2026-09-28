package com.ho.account.journalledger.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.JournalLedgerApplication;
import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantineStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalEventQuarantineRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalRuleRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = JournalLedgerApplication.class,
        properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("local")
class KafkaJournalEventQuarantineIntegrationTest {

    @Autowired KafkaJournalEventUseCase useCase;
    @Autowired JournalEventQuarantineRepository quarantineRepository;
    @Autowired JournalRuleRepository ruleRepository;
    @Autowired JournalEntryRepository journalRepository;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void unmatchedEventIsDurableAndConcurrentReplayAfterRuleRepairCreatesOneJournal() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String ruleCode = "GL18_" + suffix;
        String sourceId = "EVENT_" + suffix;
        long offset = Math.abs(UUID.randomUUID().getMostSignificantBits());
        LocalDate accountingDate = LocalDate.of(2026, 9, 28);
        Map<String, Object> event = Map.of(
                "ruleCode", ruleCode,
                "amount", "100.00",
                "lineageSourceType", "KAFKA_GL18",
                "lineageSourceId", sourceId,
                "createdBy", "service:journal-kafka-maker",
                "auditUser", "service:journal-kafka-maker");

        var processed = useCase.process(
                new KafkaJournalEventUseCase.BrokerRecord("transaction-events", 2, offset),
                event,
                accountingDate);

        assertThat(processed.disposition())
                .isEqualTo(KafkaJournalEventUseCase.ProcessingResult.Disposition.QUARANTINED);
        Long quarantineId = processed.quarantineId();
        assertThat(quarantineId).isNotNull();
        assertThat(quarantineRepository.findById(quarantineId)).get()
                .extracting(quarantine -> quarantine.getStatus())
                .isEqualTo(JournalEventQuarantineStatus.QUARANTINED);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var pendingSummary = useCase.completenessSummary();
        assertThat(pendingSummary.quarantinedCount()).isPositive();
        assertThat(pendingSummary.oldestUnresolvedAt()).isPresent();
        assertThat(statistics.getQueryExecutionCount()).isOne();

        ruleRepository.saveAndFlush(rule(ruleCode, accountingDate));

        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return useCase.replay(quarantineId, "accounting-admin");
            });
            var second = executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return useCase.replay(quarantineId, "accounting-admin");
            });
            start.countDown();

            var firstResult = first.get(20, TimeUnit.SECONDS);
            var secondResult = second.get(20, TimeUnit.SECONDS);
            assertThat(firstResult.journalEntry().getId())
                    .isEqualTo(secondResult.journalEntry().getId());
            assertThat(java.util.List.of(firstResult.newlyCreated(), secondResult.newlyCreated()))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        var resolved = quarantineRepository.findById(quarantineId).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(JournalEventQuarantineStatus.REPLAYED);
        assertThat(resolved.getJournalEntryId()).isNotNull();
        assertThat(resolved.getReplayAttempts()).isOne();
        assertThat(journalRepository.findByLineageSourceTypeAndLineageSourceId("KAFKA_GL18", sourceId))
                .singleElement()
                .extracting(entry -> entry.getId())
                .isEqualTo(resolved.getJournalEntryId());
        statistics.clear();
        var replayedSummary = useCase.completenessSummary();
        assertThat(replayedSummary.replayedCount()).isPositive();
        assertThat(statistics.getQueryExecutionCount()).isOne();
    }

    private JournalRule rule(String ruleCode, LocalDate validFrom) {
        JournalRule rule = new JournalRule();
        rule.setRuleCode(ruleCode);
        rule.setRuleName("GL18 repaired rule");
        rule.setValidFrom(validFrom);
        rule.setActive(true);
        rule.setPriority(1);
        rule.setCreatedBy("accounting-admin");
        rule.addRuleDetail(detail(JournalSide.DEBIT, "11000"));
        rule.addRuleDetail(detail(JournalSide.CREDIT, "21000"));
        return rule;
    }

    private JournalRuleDetail detail(JournalSide side, String accountCode) {
        JournalRuleDetail detail = new JournalRuleDetail();
        detail.setSide(side);
        detail.setAccountSubjectCodeExpression(accountCode);
        detail.setAmountExpression("${amount}");
        return detail;
    }
}
