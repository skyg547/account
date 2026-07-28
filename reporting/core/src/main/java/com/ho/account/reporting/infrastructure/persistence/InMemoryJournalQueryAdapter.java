package com.ho.account.reporting.infrastructure.persistence;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local reporting drill-through adapter used only in memory mode.
 *
 * <p>실제 운영 drill-through는 journal-ledger가 제공하는 `JournalQueryPort` 구현으로 전표 상세를
 * 조회해야 합니다. memory 모드는 보고서 생성과 주석 마트 API의 컨텍스트 기동을 먼저 확인하기 위한
 * 학습 모드이므로 전표 상세는 빈 결과로 반환합니다.</p>
 */
@Component
@ConditionalOnProperty(prefix = "account.reporting.persistence", name = "mode", havingValue = "memory")
public class InMemoryJournalQueryAdapter implements JournalQueryPort {

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
    public JournalDetailAggregateSummary getJournalDetailAggregate(
            LocalDate startDate,
            LocalDate endDate,
            JournalSide side) {
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
        return null;
    }
}
