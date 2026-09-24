package com.ho.account.journalledger.domain.journal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Byte-copyable regression for the audited pre-fix revision and the current implementation.
 *
 * <p>Reflection is deliberately limited to arranging the historical POSTED fixture. Every mutation
 * under test uses the public API that was present at the audited revision.</p>
 */
class JournalPostedImmutabilityAuditedRegressionTest {

    @Test
    void postedHeaderSetterRejectsWithoutChangingAccountingDate() {
        JournalEntry entry = postedEntry();
        LocalDate original = entry.getAccountingDate();

        assertThatThrownBy(() -> entry.setAccountingDate(original.plusDays(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTED");

        assertThat(entry.getAccountingDate()).isEqualTo(original);
    }

    @Test
    void postedLineSetterRejectsWithoutChangingAmount() {
        JournalEntry entry = postedEntry();
        JournalDetail detail = entry.getDetails().get(0);
        BigDecimal original = detail.getAmount();

        assertThatThrownBy(() -> detail.setAmount(original.add(BigDecimal.ONE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTED");

        assertThat(detail.getAmount()).isEqualByComparingTo(original);
    }

    @Test
    void postedCollectionMutationRejectsWithoutRemovingDetails() {
        JournalEntry entry = postedEntry();
        JournalDetail first = entry.getDetails().get(0);

        assertThatThrownBy(entry::clearDetails)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTED");

        assertThat(entry.getDetails()).hasSize(2).contains(first);
        assertThat(first.getJournalEntry()).isSameAs(entry);
    }

    private static JournalEntry postedEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.setAccountingDate(LocalDate.of(2026, 9, 25));
        entry.setCreatedBy("audited-regression");
        entry.addDetail(detail(JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100"));
        setField(entry, "status", JournalEntryStatus.POSTED);
        return entry;
    }

    private static JournalDetail detail(JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("10.00"));
        detail.setBaseAmount(new BigDecimal("10.00"));
        return detail;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Unable to arrange audited POSTED fixture", failure);
        }
    }
}
