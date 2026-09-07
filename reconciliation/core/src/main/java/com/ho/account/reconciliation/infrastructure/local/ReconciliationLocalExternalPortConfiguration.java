package com.ho.account.reconciliation.infrastructure.local;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.ledger.LedgerAggregateSummary;
import com.ho.account.contracts.ledger.LedgerBalanceSummary;
import com.ho.account.contracts.ledger.LedgerQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * reconciliation 로컬 실행용 외부 조회/전표 포트.
 * 운영 데이터 원천이 없을 때는 빈 집계와 로컬 전표 번호를 반환해 API/BATCH 컨텍스트를 먼저 검증한다.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(
        prefix = "reconciliation.journal-ledger.remote",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true)
public class ReconciliationLocalExternalPortConfiguration {

    private final AtomicLong journalSequence = new AtomicLong(1L);

    @Bean
    @ConditionalOnMissingBean
    JournalQueryPort reconciliationLocalJournalQueryPort() {
        return new JournalQueryPort() {
            @Override
            public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
                return List.of();
            }

            @Override
            public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
                return List.of();
            }

            @Override
            public List<JournalDetailSummary> getJournalDetailsByAccountCodes(
                    LocalDate startDate,
                    LocalDate endDate,
                    List<String> accountCodes) {
                return List.of();
            }

            @Override
            public JournalDetailAggregateSummary getJournalDetailAggregate(LocalDate startDate, LocalDate endDate, JournalSide side) {
                return new JournalDetailAggregateSummary(0L, BigDecimal.ZERO);
            }

            @Override
            public JournalDetailAggregateSummary getJournalDetailAggregateByAccount(
                    LocalDate startDate,
                    LocalDate endDate,
                    JournalSide side,
                    String accountCode) {
                return new JournalDetailAggregateSummary(0L, BigDecimal.ZERO);
            }

            @Override
            public JournalSummary getJournalSummary(Long journalEntryId) {
                JournalSummary summary = new JournalSummary();
                summary.setId(journalEntryId);
                summary.setSlipNo("LOCAL-RECON-" + journalEntryId);
                summary.setAccountingDate(LocalDate.now());
                summary.setStatus("LOCAL");
                return summary;
            }

            @Override
            public Optional<JournalSummary> findBySlipNo(String slipNo) {
                return Optional.empty();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    JournalPostingPort reconciliationLocalJournalPostingPort() {
        return new JournalPostingPort() {
            @Override
            public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
                long id = journalSequence.getAndIncrement();
                return new JournalPostingResult(id, "LOCAL-RECON-" + id, "DRAFT");
            }

            @Override
            public void approveAndPost(Long journalEntryId, String actor) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    LedgerQueryPort reconciliationLocalLedgerQueryPort() {
        return new LedgerQueryPort() {
            @Override
            public List<LedgerBalanceSummary> getGlBalanceSummaries(
                    LocalDate startDate,
                    LocalDate endDate,
                    String accountCode,
                    String currencyCode) {
                return List.of();
            }

            @Override
            public List<LedgerBalanceSummary> getSlBalanceSummaries(
                    LocalDate startDate,
                    LocalDate endDate,
                    String accountCode,
                    String businessPartnerCode,
                    String departmentCode,
                    String currencyCode) {
                return List.of();
            }

            @Override
            public LedgerAggregateSummary calculateLedgerSummary(
                    LocalDate startDate,
                    LocalDate endDate,
                    String accountCode,
                    String currencyCode,
                    String amountBasis) {
                return new LedgerAggregateSummary(0L, BigDecimal.ZERO);
            }
        };
    }
}
