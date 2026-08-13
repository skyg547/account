package com.ho.account.closing.infrastructure.local;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [Closing 모듈 로컬 및 테스트용 외부 포트 기본 설정 (ClosingLocalExternalPortConfiguration)]
 *
 * closing 모듈이 독립된 Bounded Context로 구동될 때,
 * journal-ledger 연동 포트(JournalPostingPort, JournalQueryPort)의
 * 빈이 정의되어 있지 않은 경우 런타임/테스트 오류를 방지하기 위해 기본 빈을 등록합니다.
 */
@Configuration
@Profile("local")
public class ClosingLocalExternalPortConfiguration {

    private final AtomicLong journalSequence = new AtomicLong(1L);

    @Bean
    @ConditionalOnMissingBean
    public JournalPostingPort closingLocalJournalPostingPort() {
        return new JournalPostingPort() {
            @Override
            public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
                long id = journalSequence.getAndIncrement();
                return new JournalPostingResult(id, command.slipNo() != null ? command.slipNo() : "LOCAL-CLOSING-" + id, "DRAFT");
            }

            @Override
            public void approveAndPost(Long journalEntryId, String actor) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public JournalQueryPort closingLocalJournalQueryPort() {
        return new JournalQueryPort() {
            @Override
            public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
                return Collections.emptyList();
            }

            @Override
            public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
                return Collections.emptyList();
            }

            @Override
            public List<JournalDetailSummary> getJournalDetailsByAccountCodes(LocalDate startDate, LocalDate endDate, List<String> accountCodes) {
                return Collections.emptyList();
            }

            @Override
            public JournalDetailAggregateSummary getJournalDetailAggregate(LocalDate startDate, LocalDate endDate, JournalSide side) {
                return new JournalDetailAggregateSummary(0L, BigDecimal.ZERO);
            }

            @Override
            public JournalDetailAggregateSummary getJournalDetailAggregateByAccount(LocalDate startDate, LocalDate endDate, JournalSide side, String accountCode) {
                return new JournalDetailAggregateSummary(0L, BigDecimal.ZERO);
            }

            @Override
            public JournalSummary getJournalSummary(Long journalEntryId) {
                JournalSummary summary = new JournalSummary();
                summary.setId(journalEntryId);
                summary.setSlipNo("SLIP-" + journalEntryId);
                summary.setStatus("DRAFT");
                return summary;
            }

            @Override
            public Optional<JournalSummary> findBySlipNo(String slipNo) {
                return Optional.empty();
            }
        };
    }
}
