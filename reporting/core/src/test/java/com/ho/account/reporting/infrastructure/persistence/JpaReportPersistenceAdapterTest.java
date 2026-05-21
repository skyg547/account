package com.ho.account.reporting.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import com.ho.account.reporting.domain.model.RegulatoryFilingLine;
import com.ho.account.reporting.domain.model.RegulatoryReportMapping;
import com.ho.account.reporting.domain.model.ReportLine;
import com.ho.account.reporting.domain.model.ReportLineMapping;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = JpaReportPersistenceAdapterTest.JpaTestConfiguration.class)
class JpaReportPersistenceAdapterTest {

    @Autowired
    private JpaReportLineMappingAdapter lineMappingAdapter;

    @Autowired
    private JpaReportSnapshotAdapter snapshotAdapter;

    @Autowired
    private ReportLineMappingJpaRepository lineMappingRepository;

    @Autowired
    private JpaRegulatoryReportSubmissionAdapter regulatoryReportSubmissionAdapter;

    @Autowired
    private JpaDisclosureNoteMartAdapter disclosureNoteMartAdapter;

    @Autowired
    private JpaRegulatoryReportMappingAdapter regulatoryReportMappingAdapter;

    @Autowired
    private JpaRegulatoryFilingAdapter regulatoryFilingAdapter;

    @Test
    void loadMappings_groupsEffectiveAccountRowsByReportLine() {
        lineMappingRepository.deleteAll();
        lineMappingRepository.flush();
        LocalDate validFrom = LocalDate.of(2020, 1, 1);
        lineMappingRepository.saveAll(List.of(
                new ReportLineMappingJpaEntity(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        "ASSET_CASH",
                        "현금 및 현금성자산",
                        "101",
                        "3",
                        1,
                        10,
                        validFrom,
                        null),
                new ReportLineMappingJpaEntity(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        "ASSET_CASH",
                        "현금 및 현금성자산",
                        "102",
                        "3",
                        1,
                        10,
                        validFrom,
                        null),
                new ReportLineMappingJpaEntity(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        "LIABILITY_DEPOSIT",
                        "예수부채",
                        "201",
                        "8",
                        1,
                        20,
                        validFrom,
                        null),
                new ReportLineMappingJpaEntity(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        "EXPIRED_LINE",
                        "만료 항목",
                        "999",
                        "",
                        1,
                        30,
                        validFrom,
                        LocalDate.of(2025, 12, 31))));

        List<ReportLineMapping> mappings = lineMappingAdapter.loadMappings(
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0));

        assertThat(mappings)
                .extracting(ReportLineMapping::lineCode)
                .containsExactly("ASSET_CASH", "LIABILITY_DEPOSIT");
        assertThat(mappings.get(0).accountCodes()).containsExactly("101", "102");
        assertThat(mappings.get(1).accountCodes()).containsExactly("201");
    }

    @Test
    void saveFinalized_replacesAndLoadsFinalSnapshot() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        snapshotAdapter.saveFinalized(finalizedStatement("ST-001", baseDate, "1000000"));
        snapshotAdapter.saveFinalized(finalizedStatement("ST-002", baseDate, "1250000"));

        FinancialStatement loaded = snapshotAdapter.findFinalizedStatement(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate)
                .orElseThrow();

        assertThat(loaded.getStatementId()).isEqualTo("ST-002");
        assertThat(loaded.getStatus()).isEqualTo(FinancialStatement.StatementStatus.FINAL);
        assertThat(loaded.getLines()).hasSize(1);
        assertThat(loaded.getLines().get(0).getCurrentAmount()).isEqualByComparingTo("1250000");
        assertThat(loaded.getLines().get(0).getPreviousAmount()).isEqualByComparingTo("800000");
    }

    @Test
    void regulatorySubmissionAdapter_incrementsVersionPerStatementTypeAndBaseDate() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        FinancialStatement statement = finalizedStatement("ST-001", baseDate, "1250000");

        assertThat(regulatoryReportSubmissionAdapter.nextVersion(
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate)).isEqualTo(1);

        regulatoryReportSubmissionAdapter.save(com.ho.account.reporting.domain.model.RegulatoryReportSubmission.ready(
                "SUB-001",
                statement,
                1,
                "tester",
                null,
                LocalDateTime.of(2026, 4, 1, 9, 0)));

        assertThat(regulatoryReportSubmissionAdapter.nextVersion(
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate)).isEqualTo(2);
    }

    @Test
    void disclosureNoteMartAdapter_replacesAndLoadsMartEntries() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        FinancialStatement statement = finalizedStatement("ST-001", baseDate, "1250000");
        DisclosureNoteMart firstMart = DisclosureNoteMart.fromStatement(
                statement,
                "tester",
                LocalDateTime.of(2026, 4, 1, 9, 0));
        DisclosureNoteMart secondMart = DisclosureNoteMart.fromStatement(
                statement,
                "tester",
                LocalDateTime.of(2026, 4, 1, 10, 0));

        disclosureNoteMartAdapter.replace(firstMart);
        disclosureNoteMartAdapter.replace(secondMart);

        DisclosureNoteMart loaded = disclosureNoteMartAdapter.find(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate)
                .orElseThrow();

        assertThat(loaded.getMartId()).isEqualTo(secondMart.getMartId());
        assertThat(loaded.getEntries()).hasSize(1);
        assertThat(loaded.getEntries().get(0).getNoteCategory()).isEqualTo(NoteCategory.CURRENCY);
        assertThat(loaded.getEntries().get(0).getCurrentAmount()).isEqualByComparingTo("1250000");
    }

    @Test
    void regulatoryReportMappingAdapter_loadsEffectiveMappingsFromSeed() {
        List<RegulatoryReportMapping> mappings = regulatoryReportMappingAdapter.loadMappings(
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0));

        assertThat(mappings)
                .extracting(RegulatoryReportMapping::fieldCode)
                .contains("CASH_AND_CASH_EQUIVALENTS", "DEPOSIT_LIABILITIES");
    }

    @Test
    void regulatoryFilingAdapter_savesAndLoadsLatestFiling() {
        LocalDateTime baseDate = LocalDateTime.of(2026, 3, 31, 0, 0);
        RegulatoryFiling first = regulatoryFiling("FILING-001", baseDate, "RECEIPT-001", "1000000");
        RegulatoryFiling second = regulatoryFiling("FILING-002", baseDate, "RECEIPT-002", "1250000");

        regulatoryFilingAdapter.save(first);
        regulatoryFilingAdapter.save(second);

        RegulatoryFiling loaded = regulatoryFilingAdapter.findLatest(
                        FinancialStatement.StatementType.BALANCE_SHEET,
                        baseDate)
                .orElseThrow();

        assertThat(loaded.getFilingId()).isEqualTo("FILING-002");
        assertThat(loaded.getRegulatorReceiptId()).isEqualTo("RECEIPT-002");
        assertThat(loaded.getLines()).hasSize(1);
        assertThat(loaded.getLines().get(0).currentAmount()).isEqualByComparingTo("1250000");
    }

    private static FinancialStatement finalizedStatement(
            String statementId,
            LocalDateTime baseDate,
            String currentAmount) {
        FinancialStatement statement = new FinancialStatement(
                statementId,
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate);
        statement.addLine(new ReportLine(
                "ASSET_CASH",
                "현금 및 현금성자산",
                new BigDecimal(currentAmount),
                new BigDecimal("800000"),
                "3",
                1));
        statement.finalizeStatement();
        return statement;
    }

    private static RegulatoryFiling regulatoryFiling(
            String filingId,
            LocalDateTime baseDate,
            String receiptId,
            String currentAmount) {
        return RegulatoryFiling.restored(
                filingId,
                "SUB-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                baseDate,
                1,
                "FSS",
                "tester",
                "FILING-001".equals(filingId)
                        ? LocalDateTime.of(2026, 4, 1, 9, 0)
                        : LocalDateTime.of(2026, 4, 1, 10, 0),
                RegulatoryFiling.FilingStatus.ACCEPTED,
                receiptId,
                "Accepted",
                List.of(new RegulatoryFilingLine(
                        "FSS_BS_DISCLOSURE",
                        "CASH_AND_CASH_EQUIVALENTS",
                        "현금 및 현금성자산",
                        "3",
                        "ASSET_CASH",
                        "Cash",
                        new BigDecimal(currentAmount),
                        new BigDecimal("800000"),
                        10)));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = ReportLineMappingJpaRepository.class)
    @EntityScan(basePackageClasses = {
            ReportLineMappingJpaEntity.class,
            ReportSnapshotHeaderJpaEntity.class,
            ReportSnapshotDetailJpaEntity.class,
            RegulatoryReportSubmissionJpaEntity.class,
            DisclosureNoteMartJpaEntity.class,
            RegulatoryReportMappingJpaEntity.class,
            RegulatoryFilingJpaEntity.class
    })
    @Import({
            JpaReportLineMappingAdapter.class,
            JpaReportSnapshotAdapter.class,
            JpaRegulatoryReportSubmissionAdapter.class,
            JpaDisclosureNoteMartAdapter.class,
            JpaRegulatoryReportMappingAdapter.class,
            JpaRegulatoryFilingAdapter.class
    })
    static class JpaTestConfiguration {
    }
}
