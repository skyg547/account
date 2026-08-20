package com.ho.account.reporting.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryJournalQueryAdapterTest {

    private InMemoryJournalQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryJournalQueryAdapter();
    }

    @Test
    @DisplayName("인메모리 모드에서 단건 전표 조회가 UnsupportedOperationException 없이 안전한 요약 객체를 반환한다")
    void getJournalSummaryReturnsInMemoryFallback() {
        JournalSummary summary = adapter.getJournalSummary(1001L);

        assertThat(summary).isNotNull();
        assertThat(summary.getId()).isEqualTo(1001L);
        assertThat(summary.getSlipNo()).isEqualTo("MEM-JOURNAL-1001");
        assertThat(summary.getStatus()).isEqualTo("POSTED");
        assertThat(summary.getCurrencyCode()).isEqualTo("KRW");
        assertThat(summary.getAccountingDate()).isNotNull();
    }

    @Test
    @DisplayName("전표 ID가 null인 경우 null을 반환한다")
    void getJournalSummaryReturnsNullForNullId() {
        JournalSummary summary = adapter.getJournalSummary(null);

        assertThat(summary).isNull();
    }

    @Test
    @DisplayName("인메모리 모드의 집계 및 목록 조회 기본값이 올바르게 동작한다")
    void aggregateAndListQueriesReturnEmptyDefaults() {
        LocalDate now = LocalDate.now();
        assertThat(adapter.getJournalSummaries(now, now)).isEmpty();
        assertThat(adapter.getJournalDetails(1L)).isEmpty();
        assertThat(adapter.getJournalDetailsByAccountCodes(now, now, List.of("10100"))).isEmpty();

        JournalDetailAggregateSummary aggregate =
                adapter.getJournalDetailAggregate(now, now, JournalSide.DEBIT);
        assertThat(aggregate.getDetailCount()).isEqualTo(0L);
        assertThat(aggregate.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(adapter.findBySlipNo("SLIP-001")).isEmpty();
    }
}
