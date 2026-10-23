package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.infrastructure.source.JdbcEclAllowanceResultAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcEclAllowanceResultAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcEclAllowanceResultAdapter adapter;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:closing-ecl-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        adapter = new JdbcEclAllowanceResultAdapter(jdbcTemplate);
        jdbcTemplate.execute("""
                CREATE TABLE allowance_summary (
                    id BIGINT PRIMARY KEY,
                    base_date DATE NOT NULL,
                    run_id VARCHAR(80) NOT NULL,
                    model_version VARCHAR(80) NOT NULL,
                    legal_entity_code VARCHAR(20) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    exposure_account_code VARCHAR(20) NOT NULL,
                    allowance_account_code VARCHAR(20) NOT NULL,
                    bad_debt_expense_account_code VARCHAR(20) NOT NULL,
                    reversal_income_account_code VARCHAR(20) NOT NULL,
                    target_allowance_amount NUMERIC(19,4) NOT NULL,
                    source_exposure_amount NUMERIC(19,4) NOT NULL,
                    stage1_allowance_amount NUMERIC(19,4) NOT NULL,
                    stage2_allowance_amount NUMERIC(19,4) NOT NULL,
                    stage3_allowance_amount NUMERIC(19,4) NOT NULL
                )
                """);
    }

    @Test
    void aggregatesExposureRowsBeforeReturningPostingGroups() {
        insert(1, "12000", "600.00", "6000.00");
        insert(2, "12100", "400.00", "4000.00");

        List<EclAllowanceSummary> result = adapter.loadSummaries(LocalDate.of(2026, 5, 31));

        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.runId()).isEqualTo("RUN-1");
            assertThat(summary.allowanceAccountCode()).isEqualTo("129100");
            assertThat(summary.targetAllowanceAmount()).isEqualByComparingTo("1000.00");
            assertThat(summary.sourceExposureAmount()).isEqualByComparingTo("10000.00");
            assertThat(summary.stage1AllowanceAmount()).isEqualByComparingTo("1000.00");
        });
    }

    @Test
    void apiGroupCapFailsInsteadOfReturningATruncatedEclSnapshot() {
        insert(1, "12000", "600.00", "6000.00");
        jdbcTemplate.update("""
                INSERT INTO allowance_summary VALUES (
                    2, DATE '2026-05-31', 'RUN-1', 'MODEL-1', 'ENTITY-1', 'EUR', '12100',
                    '129200', '550100', '480100', 400.00, 4000.00, 400.00, 0, 0)
                """);
        JdbcEclAllowanceResultAdapter bounded =
                new JdbcEclAllowanceResultAdapter(jdbcTemplate, 1, 60);

        assertThatThrownBy(() -> bounded.loadSummaries(LocalDate.of(2026, 5, 31)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ECL summary groups exceed API hard cap of 1");
    }

    @Test
    void rejectsOffsettingUnreconciledSourceRowsBeforeGrouping() {
        insert(1, "12000", "100.0000", "1000.0000");
        insert(2, "12100", "100.0000", "1000.0000");
        jdbcTemplate.update("UPDATE allowance_summary SET stage1_allowance_amount = 99.9999 WHERE id = 1");
        jdbcTemplate.update("UPDATE allowance_summary SET stage1_allowance_amount = 100.0001 WHERE id = 2");

        assertThatThrownBy(() -> adapter.loadSummaries(LocalDate.of(2026, 5, 31)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source row reconciliation");
    }

    @Test
    void preservesExactGroupedStageTotals() {
        insert(1, "12000", "33.3333", "1000.0000");
        insert(2, "12100", "66.6668", "1000.0000");

        assertThat(adapter.loadSummaries(LocalDate.of(2026, 5, 31)))
                .singleElement().satisfies(summary -> {
                    assertThat(summary.targetAllowanceAmount()).isEqualByComparingTo("100.0001");
                    assertThat(summary.stage1AllowanceAmount()).isEqualByComparingTo("100.0001");
                });
    }

    @Test
    void rejectsZeroExposureWithPositiveTargetBeforeGrouping() {
        insert(1, "12000", "1.0000", "0.0000");

        assertThatThrownBy(() -> adapter.loadSummaries(LocalDate.of(2026, 5, 31)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source row reconciliation");
    }

    @Test
    void rejectsNullStageSourceRowBeforeGrouping() {
        insert(1, "12000", "100.0000", "1000.0000");
        jdbcTemplate.execute("ALTER TABLE allowance_summary ALTER COLUMN stage2_allowance_amount DROP NOT NULL");
        jdbcTemplate.update("UPDATE allowance_summary SET stage2_allowance_amount = NULL WHERE id = 1");

        assertThatThrownBy(() -> adapter.loadSummaries(LocalDate.of(2026, 5, 31)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source row reconciliation");
    }

    private void insert(int id, String exposureAccountCode, String target, String exposure) {
        jdbcTemplate.update("""
                INSERT INTO allowance_summary VALUES (
                    ?, DATE '2026-05-31', 'RUN-1', 'MODEL-1', 'ENTITY-1', 'USD', ?,
                    '129100', '550100', '480100', ?, ?, ?, 0, 0)
                """,
                id,
                exposureAccountCode,
                target,
                exposure,
                target);
    }
}
