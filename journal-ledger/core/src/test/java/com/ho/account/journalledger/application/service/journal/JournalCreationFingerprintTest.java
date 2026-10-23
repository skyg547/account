package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class JournalCreationFingerprintTest {

    @Test
    void canonicalFingerprintIgnoresLineOrderAndEquivalentDecimalRepresentations() {
        JournalEntry first = journal("100", "1", List.of(
                detail(JournalSide.DEBIT, "10100", "100", "100.0", "D-1", "BP-1", "cash"),
                detail(JournalSide.CREDIT, "40100", "100.00", "100.00", "D-2", "BP-2", "revenue")));
        JournalEntry reorderedAndRescaled = journal("100.00000000", "1.00000000", List.of(
                detail(JournalSide.CREDIT, "40100", "100.0", "100", "D-2", "BP-2", "revenue"),
                detail(JournalSide.DEBIT, "10100", "100.00", "100.000", "D-1", "BP-1", "cash")));

        String expected = JournalCreationFingerprint.calculate(first);

        assertThat(expected).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(JournalCreationFingerprint.calculate(reorderedAndRescaled)).isEqualTo(expected);
    }

    @Test
    void generatedSlipAndLifecycleStateDoNotChangeCreationFingerprint() {
        JournalEntry first = standardJournal();
        JournalEntry withGeneratedFields = standardJournal();
        withGeneratedFields.setSlipNo("JE-20260925-GENERATED");
        withGeneratedFields.initializeDraft();

        assertThat(JournalCreationFingerprint.calculate(withGeneratedFields))
                .isEqualTo(JournalCreationFingerprint.calculate(first));
    }

    @Test
    void omittedKrwDefaultsAndTextualEntryTypeVariantsHaveOneCanonicalFingerprint() {
        JournalEntry omittedDefaults = standardJournal();
        omittedDefaults.setEntryType(null);
        omittedDefaults.setCurrencyCode(null);
        omittedDefaults.setExchangeRate(null);
        JournalEntry explicitDefaults = standardJournal();
        explicitDefaults.setEntryType(" normal ");
        explicitDefaults.setCurrencyCode(" krw ");
        explicitDefaults.setExchangeRate(BigDecimal.ONE);

        assertThat(JournalCreationFingerprint.calculate(omittedDefaults))
                .isEqualTo(JournalCreationFingerprint.calculate(explicitDefaults));
    }

    @TestFactory
    Stream<DynamicTest> eachPersistedFinancialOrAuditFieldChangesFingerprint() {
        return Stream.of(
                mutation("slip date", entry -> entry.setSlipDate(LocalDate.of(2026, 9, 26))),
                mutation("accounting date", entry -> entry.setAccountingDate(LocalDate.of(2026, 9, 26))),
                mutation("description", entry -> entry.setDescription("different description")),
                mutation("entry type", entry -> entry.setEntryType("ADJUSTMENT")),
                mutation("currency", entry -> entry.setCurrencyCode("USD")),
                mutation("exchange rate", entry -> entry.setExchangeRate(new BigDecimal("1.10000000"))),
                mutation("lineage type", entry -> entry.setLineageSourceType("OTHER_SOURCE")),
                mutation("lineage id", entry -> entry.setLineageSourceId("OTHER-765")),
                mutation("line side", entry -> entry.getDetails().get(0).setSide(JournalSide.CREDIT)),
                mutation("account", entry -> entry.getDetails().get(0).setAccountCode("10200")),
                mutation("amount", entry -> entry.getDetails().get(0).setAmount(new BigDecimal("101.00"))),
                mutation("base amount", entry -> entry.getDetails().get(0).setBaseAmount(new BigDecimal("101.00"))),
                mutation("department", entry -> entry.getDetails().get(0).setDepartmentCode("D-9")),
                mutation("business partner", entry -> entry.getDetails().get(0).setBusinessPartnerCode("BP-9")),
                mutation("line description", entry -> entry.getDetails().get(0).setDetailDescription("changed")),
                mutation("duplicate line multiplicity", entry -> entry.addDetail(
                        detail(JournalSide.DEBIT, "10100", "100.00", "100.00",
                                "D-1", "BP-1", "cash"))))
                .map(mutation -> DynamicTest.dynamicTest(mutation.name(), () -> {
                    JournalEntry baseline = standardJournal();
                    JournalEntry changed = standardJournal();
                    mutation.change().accept(changed);

                    assertThat(JournalCreationFingerprint.calculate(changed))
                            .as(mutation.name())
                            .isNotEqualTo(JournalCreationFingerprint.calculate(baseline));
                }));
    }

    @Test
    void differentInitialMakerChangesFingerprintWithoutMutatingImmutableIdentity() {
        JournalEntry baseline = standardJournal();
        JournalEntry changed = journal("100.00", "1.00000000", "different-maker", List.of(
                detail(JournalSide.DEBIT, "10100", "100.00", "100.00", "D-1", "BP-1", "cash"),
                detail(JournalSide.CREDIT, "40100", "100.00", "100.00", "D-2", "BP-2", "revenue")));
        changed.getDetails().forEach(detail -> detail.setAuditUser("different-maker"));

        assertThat(JournalCreationFingerprint.calculate(changed))
                .isNotEqualTo(JournalCreationFingerprint.calculate(baseline));
    }

    private static Mutation mutation(String name, Consumer<JournalEntry> change) {
        return new Mutation(name, change);
    }

    private static JournalEntry standardJournal() {
        return journal("100.00", "1.00000000", List.of(
                detail(JournalSide.DEBIT, "10100", "100.00", "100.00", "D-1", "BP-1", "cash"),
                detail(JournalSide.CREDIT, "40100", "100.00", "100.00", "D-2", "BP-2", "revenue")));
    }

    private static JournalEntry journal(String amount, String exchangeRate, List<JournalDetail> details) {
        return journal(amount, exchangeRate, "service:test-maker", details);
    }

    private static JournalEntry journal(
            String amount, String exchangeRate, String maker, List<JournalDetail> details) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.of(2026, 9, 25));
        entry.setAccountingDate(LocalDate.of(2026, 9, 25));
        entry.setDescription("issue 765 journal");
        entry.setEntryType("NORMAL");
        entry.setCurrencyCode("KRW");
        entry.setExchangeRate(new BigDecimal(exchangeRate));
        entry.setCreatedBy(maker);
        entry.setAuditUser(maker);
        entry.setLineageSourceType("ISSUE_765");
        entry.setLineageSourceId("SOURCE-765");
        details.forEach(entry::addDetail);
        // Keep the amount parameter visible in the fixture contract: both lines are expected to use it.
        entry.getDetails().forEach(detail -> detail.setAmount(new BigDecimal(amount)));
        return entry;
    }

    private static JournalDetail detail(
            JournalSide side,
            String accountCode,
            String amount,
            String baseAmount,
            String department,
            String partner,
            String description) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(baseAmount));
        detail.setDepartmentCode(department);
        detail.setBusinessPartnerCode(partner);
        detail.setDetailDescription(description);
        detail.setAuditUser("service:test-maker");
        return detail;
    }

    private record Mutation(String name, Consumer<JournalEntry> change) { }
}
