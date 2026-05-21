package com.ho.account.reporting.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry.NoteCategory;
import com.ho.account.reporting.domain.model.FinancialStatement;
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = ReportLineMappingJpaRepository.class)
    @EntityScan(basePackageClasses = {
            ReportLineMappingJpaEntity.class,
            ReportSnapshotHeaderJpaEntity.class,
            ReportSnapshotDetailJpaEntity.class,
            RegulatoryReportSubmissionJpaEntity.class,
            DisclosureNoteMartJpaEntity.class
    })
    @Import({
            JpaReportLineMappingAdapter.class,
            JpaReportSnapshotAdapter.class,
            JpaRegulatoryReportSubmissionAdapter.class,
            JpaDisclosureNoteMartAdapter.class
    })
    static class JpaTestConfiguration {
    }
}
