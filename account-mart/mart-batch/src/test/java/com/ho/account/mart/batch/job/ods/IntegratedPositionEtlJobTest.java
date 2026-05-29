package com.ho.account.mart.batch.job.ods;

import com.ho.account.shared.finance.entity.AllowanceInputPosition;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [QA] 대손충당금 입력 포지션 적재 배치(ETL) 통합 테스트.
 * ODS(원천) 데이터를 CDM(마트) 모델로 변환하여 적재하는 파이프라인을 검증합니다.
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class IntegratedPositionEtlJobTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 4, 15);

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJob(org.springframework.batch.core.Job integratedPositionEtlJob) {
        this.jobLauncherTestUtils.setJob(integratedPositionEtlJob);
    }

    @BeforeEach
    void setUp() {
        seedDemoSourceData(BASE_DATE);
    }

    @Test
    @DisplayName("demo 원천 데이터를 생성한 뒤 대손충당금 입력 포지션 배치가 완주되는지 테스트")
    public void testIntegratedPositionEtlJob() throws Exception {
        LocalDate baseDate = BASE_DATE;
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDate.toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        List<AllowanceInputPosition> results = entityManager.createQuery(
                "SELECT p FROM AllowanceInputPosition p WHERE p.baseDt = :baseDate ORDER BY p.accNo",
                AllowanceInputPosition.class)
                .setParameter("baseDate", baseDate)
                .getResultList();

        assertThat(results).hasSize(5);
        assertThat(results).extracting(AllowanceInputPosition::getAccNo)
                .containsExactly("DEMO-ACC001", "DEMO-ACC002", "DEMO-ACC003", "DEMO-ACC004", "DEMO-ACC005");

        AllowanceInputPosition defaultedCard = results.stream()
                .filter(position -> "DEMO-ACC004".equals(position.getAccNo()))
                .findFirst()
                .orElseThrow();
        assertThat(defaultedCard.getStaging().name()).isEqualTo("STAGE3");

        AllowanceInputPosition usdExposure = results.stream()
                .filter(position -> "DEMO-ACC005".equals(position.getAccNo()))
                .findFirst()
                .orElseThrow();
        assertThat(usdExposure.getMarketValue()).isEqualByComparingTo(new BigDecimal("13500000.0000"));

        Integer reconcileMatches = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ods_reconcile_hist WHERE base_dt = ? AND status = '정상(MATCH)'",
                Integer.class,
                java.sql.Date.valueOf(baseDate));
        assertThat(reconcileMatches).isEqualTo(4);

        Integer snapshotRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allowance_exposure_snapshots WHERE base_date = ?",
                Integer.class,
                java.sql.Date.valueOf(baseDate));
        assertThat(snapshotRows).isEqualTo(5);

        BigDecimal undrawnAmount = jdbcTemplate.queryForObject(
                "SELECT undrawn_amount FROM allowance_exposure_snapshots WHERE base_date = ? AND exposure_id = ?",
                BigDecimal.class,
                java.sql.Date.valueOf(baseDate),
                "DEMO-ACC003");
        assertThat(undrawnAmount).isEqualByComparingTo(new BigDecimal("500000000.0000"));

        String accountingAccountCode = jdbcTemplate.queryForObject(
                "SELECT accounting_account_code FROM allowance_exposure_snapshots WHERE base_date = ? AND exposure_id = ?",
                String.class,
                java.sql.Date.valueOf(baseDate),
                "DEMO-ACC001");
        assertThat(accountingAccountCode).isEqualTo("L001");
    }

    private void seedDemoSourceData(LocalDate baseDate) {
        cleanupDemoData(baseDate);

        insertProduct("DEMO-LOAN", "대손충당금 대출", "LOAN", "L001", false, new BigDecimal("1.0000"));
        insertProduct("DEMO-LIMIT", "대손충당금 약정", "OFF_BALANCE", "L003", true, new BigDecimal("0.5000"));
        insertProduct("DEMO-CARD", "대손충당금 카드", "CARD", "L004", false, new BigDecimal("1.0000"));
        insertProduct("DEMO-USD", "대손충당금 외화대출", "LOAN", "L005", false, new BigDecimal("1.0000"));

        insertCustomer("DEMO-CUST001", "정상 개인", "RETAIL", "KR", "A", false, "BR001");
        insertCustomer("DEMO-CUST002", "주의 법인", "CORPORATE", "KR", "BBB", false, "BR001");
        insertCustomer("DEMO-CUST003", "한도 중소기업", "SME", "KR", "BBB", true, "BR002");
        insertCustomer("DEMO-CUST004", "부도 카드고객", "RETAIL", "KR", "CCC", false, "BR003");
        insertCustomer("DEMO-CUST005", "외화 차주", "CORPORATE", "US", "A", false, "BR004");

        insertLedger(baseDate, "DEMO-ACC001", "DEMO-CUST001", "DEMO-LOAN", "KRW",
                new BigDecimal("100000000.0000"), new BigDecimal("100000000.0000"), 0, "BR001", "ENTITY01");
        insertLedger(baseDate, "DEMO-ACC002", "DEMO-CUST002", "DEMO-LOAN", "KRW",
                new BigDecimal("200000000.0000"), new BigDecimal("200000000.0000"), 35, "BR001", "ENTITY01");
        insertLedger(baseDate, "DEMO-ACC003", "DEMO-CUST003", "DEMO-LIMIT", "KRW",
                new BigDecimal("100000000.0000"), new BigDecimal("600000000.0000"), 0, "BR002", "ENTITY02");
        insertLedger(baseDate, "DEMO-ACC004", "DEMO-CUST004", "DEMO-CARD", "KRW",
                new BigDecimal("5000000.0000"), new BigDecimal("5000000.0000"), 95, "BR003", "ENTITY03");
        insertLedger(baseDate, "DEMO-ACC005", "DEMO-CUST005", "DEMO-USD", "USD",
                new BigDecimal("10000.0000"), new BigDecimal("10000.0000"), 0, "BR004", "ENTITY04");

        insertGeneralLedger(baseDate, "L001", "KRW", new BigDecimal("300000000.0000"), "BR001");
        insertGeneralLedger(baseDate, "L003", "KRW", new BigDecimal("100000000.0000"), "BR002");
        insertGeneralLedger(baseDate, "L004", "KRW", new BigDecimal("5000000.0000"), "BR003");
        insertGeneralLedger(baseDate, "L005", "USD", new BigDecimal("10000.0000"), "BR004");

        jdbcTemplate.update("""
                INSERT INTO market_exchange_rate (
                    base_dt, base_currency, quote_currency, base_rate, bid_rate, ask_rate, source, created_at
                ) VALUES (?, 'USD', 'KRW', ?, ?, ?, 'TEST', CURRENT_TIMESTAMP)
                """,
                java.sql.Date.valueOf(baseDate),
                new BigDecimal("1350.0000"),
                new BigDecimal("1349.0000"),
                new BigDecimal("1351.0000"));
    }

    private void cleanupDemoData(LocalDate baseDate) {
        jdbcTemplate.update("DELETE FROM allowance_exposure_snapshots WHERE base_date = ?", java.sql.Date.valueOf(baseDate));
        jdbcTemplate.update("DELETE FROM allowance_input_positions WHERE base_dt = ?", java.sql.Date.valueOf(baseDate));
        jdbcTemplate.update("DELETE FROM ods_reconcile_hist WHERE base_dt = ?", java.sql.Date.valueOf(baseDate));
        jdbcTemplate.update("DELETE FROM ods_balance_hist WHERE base_dt = ?", java.sql.Date.valueOf(baseDate));
        jdbcTemplate.update("DELETE FROM ods_general_ledger WHERE base_dt = ?", java.sql.Date.valueOf(baseDate));
        jdbcTemplate.update("DELETE FROM market_exchange_rate WHERE base_dt = ?", java.sql.Date.valueOf(baseDate));
        jdbcTemplate.update("DELETE FROM ods_acc_ledger WHERE acc_no LIKE 'DEMO-ACC%'");
        jdbcTemplate.update("DELETE FROM ods_customer_mst WHERE customer_code LIKE 'DEMO-CUST%'");
        jdbcTemplate.update("DELETE FROM ods_product_mst WHERE prod_cd LIKE 'DEMO-%'");
    }

    private void insertProduct(String productCode,
                               String productName,
                               String productCategory,
                               String subjectCode,
                               boolean offBalance,
                               BigDecimal ccf) {
        jdbcTemplate.update("""
                INSERT INTO ods_product_mst (
                    prod_cd, prod_nm, prod_category, subj_cd, default_rate_type, rate_type,
                    default_payment_freq, payment_freq, is_excluded, is_off_balance, default_ccf, created_at
                ) VALUES (?, ?, ?, ?, 'FIXED', 'FIXED', 1, 1, FALSE, ?, ?, CURRENT_TIMESTAMP)
                """,
                productCode,
                productName,
                productCategory,
                subjectCode,
                offBalance,
                ccf);
    }

    private void insertCustomer(String customerCode,
                                String customerName,
                                String customerType,
                                String countryCode,
                                String internalRating,
                                boolean sme,
                                String branchCode) {
        jdbcTemplate.update("""
                INSERT INTO ods_customer_mst (
                    customer_code, biz_no, cust_nm, cust_type, country_cd, internal_rating,
                    rating_cd, external_rating, industry_cd, industry_nm, is_sme, branch_cd, credit_status_cd
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'NORMAL')
                """,
                customerCode,
                "BIZ-" + customerCode,
                customerName,
                customerType,
                countryCode,
                internalRating,
                internalRating,
                internalRating,
                "IND-TEST",
                "테스트 산업",
                sme,
                branchCode);
    }

    private void insertLedger(LocalDate baseDate,
                              String accountNo,
                              String customerCode,
                              String productCode,
                              String currency,
                              BigDecimal outstandingAmount,
                              BigDecimal limitAmount,
                              int delinquentDays,
                              String branchCode,
                              String businessUnitCode) {
        jdbcTemplate.update("""
                INSERT INTO ods_acc_ledger (
                    acc_no, customer_code, prod_cd, currency, outstd_amt, limit_amt, int_rate,
                    base_rate_cd, spread, next_reset_dt, open_dt, maturity_dt, delinquent_days,
                    repayment_method, grace_period, repayment_freq, branch_cd, biz_unit_cd, is_active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'KORIBOR_3M', ?, ?, ?, ?, ?, 'BULLET', 0, 1, ?, ?, TRUE)
                """,
                accountNo,
                customerCode,
                productCode,
                currency,
                outstandingAmount,
                limitAmount,
                new BigDecimal("4.500000"),
                new BigDecimal("1.000000"),
                java.sql.Date.valueOf(baseDate.plusMonths(3)),
                java.sql.Date.valueOf(baseDate.minusYears(1)),
                java.sql.Date.valueOf(baseDate.plusYears(3)),
                delinquentDays,
                branchCode,
                businessUnitCode);

        jdbcTemplate.update("""
                INSERT INTO ods_balance_hist (base_dt, account_no, currency, balance)
                VALUES (?, ?, ?, ?)
                """,
                java.sql.Date.valueOf(baseDate),
                accountNo,
                currency,
                outstandingAmount);
    }

    private void insertGeneralLedger(LocalDate baseDate,
                                     String subjectCode,
                                     String currency,
                                     BigDecimal balance,
                                     String branchCode) {
        jdbcTemplate.update("""
                INSERT INTO ods_general_ledger (base_dt, gl_code, currency, balance, branch_cd)
                VALUES (?, ?, ?, ?, ?)
                """,
                java.sql.Date.valueOf(baseDate),
                subjectCode,
                currency,
                balance,
                branchCode);
    }
}

