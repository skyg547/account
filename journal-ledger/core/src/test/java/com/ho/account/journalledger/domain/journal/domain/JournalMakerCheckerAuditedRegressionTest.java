package com.ho.account.journalledger.domain.journal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Byte-copyable regression that uses only the JournalEntry API present at audited a97d10ab. */
class JournalMakerCheckerAuditedRegressionTest {

    @Test
    void makerCannotApproveOwnDraftAndFailureDoesNotMutateIt() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.setCreatedBy("maker");
        entry.addDetail(detail(JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100"));
        entry.initializeDraft();

        assertThatThrownBy(() -> entry.approve("maker"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.DRAFT);
        assertThat(entry.getCreatedBy()).isEqualTo("maker");
        assertThat(entry.getAuditUser()).isNull();
    }

    private JournalDetail detail(JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("10.00"));
        detail.setBaseAmount(new BigDecimal("10.00"));
        return detail;
    }
}
