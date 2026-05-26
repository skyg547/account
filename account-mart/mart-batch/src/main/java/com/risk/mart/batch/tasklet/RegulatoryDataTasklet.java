package com.risk.mart.batch.tasklet;

import com.risk.mart.core.support.BatchParameterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [규제/마스터] 전사 마스터 및 사무 정보 생성 태스크릿
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 리스크 산출 전, 은행의 기초 체력을 확인하는 단계입니다.
 * '오늘의 기준금리', '회계 계정과목', '오늘이 영업일인지' 등을 먼저 확정해야
 * 이후의 모든 배분 로직과 리스크 산출이 정확해집니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegulatoryDataTasklet implements Tasklet {

    private static final String DEMO_BRANCH_CODE = "BR-DEMO";

    private final JdbcTemplate jdbcTemplate;

    @Value("${mart.batch.demo-seed.enabled:false}")
    private boolean demoSeedEnabled;

    @Override
    @org.springframework.lang.Nullable
    public RepeatStatus execute(@org.springframework.lang.NonNull StepContribution contribution,
            @org.springframework.lang.NonNull ChunkContext chunkContext) {
        LocalDate baseDt = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());

        if (!demoSeedEnabled) {
            log.info("🛡️ [규제 마스터] demo seed가 비활성화되어 기준일 {}의 외부 동기화 단계는 건너뜁니다.", baseDt);
            return RepeatStatus.FINISHED;
        }

        log.info("🛡️ [규제 마스터] demo 원천 데이터를 생성합니다. (기준일: {})", baseDt);

        ensureReferenceTables();
        deleteDemoData(baseDt);
        insertDemoProducts();
        insertDemoCustomers();
        insertReferenceData(baseDt);
        insertDemoLedgers(baseDt);
        insertDemoCollateral();
        insertDemoGeneralLedger(baseDt);

        log.info("🛡️ [규제 마스터] demo 원천 데이터 생성이 완료되었습니다. (기준일: {})", baseDt);
        return RepeatStatus.FINISHED;
    }

    private void ensureReferenceTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ods_base_rate (
                    rate_cd VARCHAR(20) NOT NULL,
                    base_dt DATE NOT NULL,
                    rate_val DECIMAL(10,6) NOT NULL,
                    PRIMARY KEY (rate_cd, base_dt)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ods_biz_day (
                    base_dt DATE PRIMARY KEY,
                    is_biz_day BOOLEAN DEFAULT TRUE,
                    holiday_nm VARCHAR(100)
                )
                """);
    }

    private void deleteDemoData(LocalDate baseDt) {
        jdbcTemplate.update("DELETE FROM ods_balance_hist WHERE base_dt = ? AND acc_no LIKE 'DEMO-ACC%'", baseDt);
        jdbcTemplate.update("DELETE FROM ods_general_ledger WHERE base_dt = ? AND br_cd = ?", baseDt, DEMO_BRANCH_CODE);
        jdbcTemplate.update("DELETE FROM market_exchange_rate WHERE base_dt = ? AND base_currency = 'USD' AND quote_currency = 'KRW'", baseDt);
        jdbcTemplate.update("DELETE FROM ods_base_rate WHERE base_dt = ? AND rate_cd IN ('KORIBOR_3M', 'USD_LIBOR_3M')", baseDt);
        jdbcTemplate.update("DELETE FROM ods_biz_day WHERE base_dt = ?", baseDt);
        jdbcTemplate.update("DELETE FROM ods_coll_mst WHERE coll_id IN ('DEMO-COLL001', 'DEMO-COLL002')");
        jdbcTemplate.update("DELETE FROM ods_acc_ledger WHERE acc_no IN ('DEMO-ACC001', 'DEMO-ACC002', 'DEMO-ACC003', 'DEMO-ACC004', 'DEMO-ACC005')");
        jdbcTemplate.update("DELETE FROM ods_customer_mst WHERE customer_code IN ('DEMO-CUST001', 'DEMO-CUST002', 'DEMO-CUST003', 'DEMO-CUST004')");
        jdbcTemplate.update("DELETE FROM ods_product_mst WHERE prod_cd IN ('DEMO-L001', 'DEMO-L002', 'DEMO-C001')");
    }

    private void insertDemoProducts() {
        jdbcTemplate.batchUpdate(
                "INSERT INTO ods_product_mst (prod_cd, prod_nm, prod_category, subj_cd, rate_type, payment_freq, is_excluded, is_off_balance, default_ccf) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                java.util.List.of(
                        new Object[] { "DEMO-L001", "일반가계대출", "LOAN", "L001", "FLOATING", 1, false, false, java.math.BigDecimal.ZERO },
                        new Object[] { "DEMO-L002", "중소기업운전자금", "LOAN", "L002", "FLOATING", 3, false, false, java.math.BigDecimal.ZERO },
                        new Object[] { "DEMO-C001", "개인신용카드", "CARD", "C001", "FLOATING", 1, false, false, new java.math.BigDecimal("0.2000") }));
    }

    private void insertDemoCustomers() {
        jdbcTemplate.batchUpdate(
                "INSERT INTO ods_customer_mst (customer_code, cust_nm, cust_type, internal_rating, rating_cd, is_sme, credit_status_cd, country_cd, industry_cd, branch_cd) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                java.util.List.of(
                        new Object[] { "DEMO-CUST001", "홍길동", "RETAIL", "A1", "1", false, "NORMAL", "KR", "RETAIL", DEMO_BRANCH_CODE },
                        new Object[] { "DEMO-CUST002", "김철수", "RETAIL", "B2", "5", false, "NORMAL", "KR", "RETAIL", DEMO_BRANCH_CODE },
                        new Object[] { "DEMO-CUST003", "(주)미래소프트", "SME", "BB+", "8", true, "NORMAL", "KR", "SME", DEMO_BRANCH_CODE },
                        new Object[] { "DEMO-CUST004", "이영희", "RETAIL", "D", "15", false, "DEFAULT", "KR", "RETAIL", DEMO_BRANCH_CODE }));
    }

    private void insertReferenceData(LocalDate baseDt) {
        jdbcTemplate.update(
                "INSERT INTO market_exchange_rate (base_dt, base_currency, quote_currency, base_rate, bid_rate, ask_rate, change_amt, change_rate, source, created_at) VALUES (?, 'USD', 'KRW', 1350.0000, 1348.0000, 1352.0000, 10.0000, 0.7400, 'DEMO', CURRENT_TIMESTAMP)",
                baseDt);
        jdbcTemplate.update("INSERT INTO ods_base_rate (rate_cd, base_dt, rate_val) VALUES ('KORIBOR_3M', ?, 0.035000)", baseDt);
        jdbcTemplate.update("INSERT INTO ods_base_rate (rate_cd, base_dt, rate_val) VALUES ('USD_LIBOR_3M', ?, 0.025000)", baseDt);
        jdbcTemplate.update("INSERT INTO ods_biz_day (base_dt, is_biz_day, holiday_nm) VALUES (?, TRUE, '정상영업일')", baseDt);
    }

    private void insertDemoLedgers(LocalDate baseDt) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO ods_acc_ledger (acc_no, customer_code, prod_cd, currency, outstd_amt, limit_amt, int_rate, base_rate_cd, spread, open_dt, maturity_dt, delinquent_days, repayment_method, branch_cd, biz_unit_cd, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                java.util.List.of(
                        new Object[] { "DEMO-ACC001", "DEMO-CUST001", "DEMO-L001", "KRW", new java.math.BigDecimal("100000000"), new java.math.BigDecimal("100000000"), new java.math.BigDecimal("0.0450"), "KORIBOR_3M", new java.math.BigDecimal("0.0100"), java.sql.Date.valueOf(LocalDate.of(2025, 1, 1)), java.sql.Date.valueOf(LocalDate.of(2027, 1, 1)), 0, "BULLET", DEMO_BRANCH_CODE, "RB", true },
                        new Object[] { "DEMO-ACC002", "DEMO-CUST002", "DEMO-L001", "KRW", new java.math.BigDecimal("50000000"), new java.math.BigDecimal("50000000"), new java.math.BigDecimal("0.0550"), "KORIBOR_3M", new java.math.BigDecimal("0.0150"), java.sql.Date.valueOf(LocalDate.of(2025, 2, 1)), java.sql.Date.valueOf(LocalDate.of(2026, 2, 1)), 45, "BULLET", DEMO_BRANCH_CODE, "RB", true },
                        new Object[] { "DEMO-ACC003", "DEMO-CUST003", "DEMO-L002", "KRW", new java.math.BigDecimal("500000000"), new java.math.BigDecimal("1000000000"), new java.math.BigDecimal("0.0600"), "KORIBOR_3M", new java.math.BigDecimal("0.0200"), java.sql.Date.valueOf(LocalDate.of(2024, 5, 15)), java.sql.Date.valueOf(LocalDate.of(2026, 5, 15)), 0, "AMORTIZING", DEMO_BRANCH_CODE, "CB", true },
                        new Object[] { "DEMO-ACC004", "DEMO-CUST004", "DEMO-C001", "KRW", new java.math.BigDecimal("10000000"), new java.math.BigDecimal("10000000"), new java.math.BigDecimal("0.1800"), "KORIBOR_3M", new java.math.BigDecimal("0.1200"), java.sql.Date.valueOf(LocalDate.of(2024, 10, 1)), java.sql.Date.valueOf(LocalDate.of(2025, 10, 1)), 120, "REVOLVING", DEMO_BRANCH_CODE, "RB", true },
                        new Object[] { "DEMO-ACC005", "DEMO-CUST001", "DEMO-L001", "USD", new java.math.BigDecimal("10000"), new java.math.BigDecimal("10000"), new java.math.BigDecimal("0.0350"), "USD_LIBOR_3M", new java.math.BigDecimal("0.0050"), java.sql.Date.valueOf(LocalDate.of(2025, 3, 1)), java.sql.Date.valueOf(LocalDate.of(2026, 3, 1)), 0, "BULLET", DEMO_BRANCH_CODE, "IB", true }));

        jdbcTemplate.batchUpdate(
                "INSERT INTO ods_balance_hist (acc_no, base_dt, cur_bal, fx_rate) VALUES (?, ?, ?, ?)",
                java.util.List.of(
                        new Object[] { "DEMO-ACC001", java.sql.Date.valueOf(baseDt), new java.math.BigDecimal("100000000"), java.math.BigDecimal.ONE },
                        new Object[] { "DEMO-ACC002", java.sql.Date.valueOf(baseDt), new java.math.BigDecimal("50000000"), java.math.BigDecimal.ONE },
                        new Object[] { "DEMO-ACC003", java.sql.Date.valueOf(baseDt), new java.math.BigDecimal("500000000"), java.math.BigDecimal.ONE },
                        new Object[] { "DEMO-ACC004", java.sql.Date.valueOf(baseDt), new java.math.BigDecimal("10000000"), java.math.BigDecimal.ONE },
                        new Object[] { "DEMO-ACC005", java.sql.Date.valueOf(baseDt), new java.math.BigDecimal("10000"), new java.math.BigDecimal("1350.0000") }));
    }

    private void insertDemoCollateral() {
        jdbcTemplate.batchUpdate(
                "INSERT INTO ods_coll_mst (coll_id, cust_cd, coll_type, coll_amt, haircut_ratio) VALUES (?, ?, ?, ?, ?)",
                java.util.List.of(
                        new Object[] { "DEMO-COLL001", "DEMO-CUST001", "REAL_ESTATE", new java.math.BigDecimal("150000000"), new java.math.BigDecimal("0.2000") },
                        new Object[] { "DEMO-COLL002", "DEMO-CUST003", "GUARANTEE", new java.math.BigDecimal("400000000"), new java.math.BigDecimal("0.1000") }));
    }

    private void insertDemoGeneralLedger(LocalDate baseDt) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO ods_general_ledger (base_dt, subj_cd, br_cd, curr_cd, net_bal) VALUES (?, ?, ?, ?, ?)",
                java.util.List.of(
                        new Object[] { java.sql.Date.valueOf(baseDt), "L001", DEMO_BRANCH_CODE, "KRW", new java.math.BigDecimal("150000000") },
                        new Object[] { java.sql.Date.valueOf(baseDt), "L002", DEMO_BRANCH_CODE, "KRW", new java.math.BigDecimal("500000000") },
                        new Object[] { java.sql.Date.valueOf(baseDt), "C001", DEMO_BRANCH_CODE, "KRW", new java.math.BigDecimal("10000000") },
                        new Object[] { java.sql.Date.valueOf(baseDt), "L001", DEMO_BRANCH_CODE, "USD", new java.math.BigDecimal("10000") }));
    }
}
