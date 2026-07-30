package com.ho.account.journalledger.domain.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeneralLedgerTest {

    @Test
    @DisplayName("승인된 전표를 lineage와 SL 차원을 보존한 불변 원장 스냅샷으로 만든다.")
    void createsImmutablePostingSnapshot() {
        JournalEntry entry = approvedEntry(true);

        GeneralLedger ledger = GeneralLedger.fromApproved(entry);

        assertThat(ledger.journalEntryId()).isEqualTo(1L);
        assertThat(ledger.currencyCode()).isEqualTo("KRW");
        assertThat(ledger.postings()).hasSize(2);
        assertThat(ledger.postings().get(0).debit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings().get(1).credit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings().get(0).businessPartnerCode()).isEqualTo("BP-001");
        assertThat(ledger.postings()).allSatisfy(posting -> {
            assertThat(posting.lineageSourceType()).isEqualTo("UNIT_TEST");
            assertThat(posting.lineageSourceId()).isEqualTo("SRC-44");
        });
        assertThatThrownBy(() -> ledger.postings().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("저장되지 않은 상세 라인은 FK lineage가 없으므로 전기 Aggregate 생성을 거부한다.")
    void rejectsTransientJournalDetail() {
        JournalEntry entry = approvedEntry(false);

        assertThatThrownBy(() -> GeneralLedger.fromApproved(entry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("먼저 저장");
    }

    @Test
    @DisplayName("저장되지 않은 전표 헤더는 GL/SL lineage의 기준 ID가 없으므로 전기를 거부한다.")
    void rejectsTransientJournalEntry() {
        JournalEntry entry = approvedEntry(true);
        entry.setId(null);

        assertThatThrownBy(() -> GeneralLedger.fromApproved(entry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("전표가 먼저 저장");
    }

    @Test
    @DisplayName("승인 전 전표는 원장 Aggregate로 승격할 수 없다.")
    void rejectsUnapprovedJournalEntry() {
        JournalEntry entry = baseEntry(true);
        entry.initializeDraft();

        assertThatThrownBy(() -> GeneralLedger.fromApproved(entry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인된 전표");
    }

    @Test
    @DisplayName("개별 저장 가능한 대형 라인의 합계는 단일 컬럼 정밀도를 넘어도 차대가 맞으면 전기한다.")
    void acceptsLargeBalancedAggregateTotal() {
        JournalEntry entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("JE-20260730-LARGE");
        entry.setSlipDate(LocalDate.of(2026, 7, 30));
        entry.setAccountingDate(LocalDate.of(2026, 7, 30));
        entry.setCurrencyCode("KRW");
        entry.addDetail(detail(11L, JournalSide.DEBIT, "10100", "50000000000000000.00"));
        entry.addDetail(detail(12L, JournalSide.DEBIT, "10200", "50000000000000000.00"));
        entry.addDetail(detail(13L, JournalSide.CREDIT, "40100", "50000000000000000.00"));
        entry.addDetail(detail(14L, JournalSide.CREDIT, "40200", "50000000000000000.00"));
        entry.initializeDraft();
        entry.approve("approver");

        GeneralLedger ledger = GeneralLedger.fromApproved(entry);

        assertThat(ledger.postings()).hasSize(4);
    }

    private JournalEntry approvedEntry(boolean persistedDetails) {
        JournalEntry entry = baseEntry(persistedDetails);
        entry.initializeDraft();
        entry.approve("approver");
        return entry;
    }

    private JournalEntry baseEntry(boolean persistedDetails) {
        JournalEntry entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("JE-20260730-0044");
        entry.setSlipDate(LocalDate.of(2026, 7, 30));
        entry.setAccountingDate(LocalDate.of(2026, 7, 30));
        entry.setCurrencyCode("krw");
        entry.setLineageSourceType("UNIT_TEST");
        entry.setLineageSourceId("SRC-44");
        entry.addDetail(detail(persistedDetails ? 11L : null, JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(persistedDetails ? 12L : null, JournalSide.CREDIT, "40100"));
        return entry;
    }

    private JournalDetail detail(Long id, JournalSide side, String accountCode) {
        return detail(id, side, accountCode, "100.00");
    }

    private JournalDetail detail(Long id, JournalSide side, String accountCode, String amount) {
        JournalDetail detail = new JournalDetail();
        detail.setId(id);
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(amount));
        detail.setBusinessPartnerCode("BP-001");
        detail.setDepartmentCode("D-10");
        detail.setDetailDescription("Issue #44");
        return detail;
    }
}
