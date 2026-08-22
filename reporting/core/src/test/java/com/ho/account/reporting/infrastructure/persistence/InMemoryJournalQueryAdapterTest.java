package com.ho.account.reporting.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
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
    @DisplayName("null ID 전달 시 IllegalArgumentException이 발생한다")
    void getJournalSummary_withNullId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> adapter.getJournalSummary(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("journalEntryId is required");
    }

    @Test
    @DisplayName("존재하지 않는 전표 ID 단건 조회 시 NoSuchElementException이 발생한다")
    void getJournalSummary_withNonExistentId_throwsNoSuchElementException() {
        assertThatThrownBy(() -> adapter.getJournalSummary(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Journal entry not found: 999");
    }

    @Test
    @DisplayName("전표 번호(slipNo)로 조회 시 빈 Optional을 반환한다")
    void findBySlipNo_returnsEmptyOptional() {
        assertThat(adapter.findBySlipNo("SLIP-202608-0001")).isEmpty();
        assertThat(adapter.findBySlipNo(null)).isEmpty();
    }

    @Test
    @DisplayName("목록 및 상세 조회는 안전하게 빈 리스트를 반환한다")
    void listQueries_returnEmptyLists() {
        LocalDate today = LocalDate.of(2026, 8, 21);

        assertThat(adapter.getJournalSummaries(today, today)).isEmpty();
        assertThat(adapter.getJournalDetails(1L)).isEmpty();
        assertThat(adapter.getJournalDetailsByAccountCodes(today, today, List.of("10100"))).isEmpty();
    }

    @Test
    @DisplayName("집계 요약 조회는 카운트 0, 금액 0의 요약 객체를 반환한다")
    void aggregateQueries_returnZeroSummary() {
        LocalDate today = LocalDate.of(2026, 8, 21);

        JournalDetailAggregateSummary summary1 = adapter.getJournalDetailAggregate(today, today, JournalSide.DEBIT);
        assertThat(summary1.getDetailCount()).isEqualTo(0L);
        assertThat(summary1.getTotalAmount()).isEqualTo(BigDecimal.ZERO);

        JournalDetailAggregateSummary summary2 = adapter.getJournalDetailAggregateByAccount(today, today, JournalSide.CREDIT, "10100");
        assertThat(summary2.getDetailCount()).isEqualTo(0L);
        assertThat(summary2.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
    }
}
