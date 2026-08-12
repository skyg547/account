package com.ho.account.mart.batch.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/**
 * [금융 데이터 마트 배치 결정론적 시드 데이터 서비스 (AccountMartDemoFixtureService)]
 *
 * 💡 [아키텍처 및 금융 데이터 마트 설계 패턴 Guide]
 * 1. 데이터 마트(Data Mart) 및 IFRS9 / ALM 배치 생명주기:
 *    금융 기관의 데이터 마트는 원천 시스템(ODS: Operational Data Store)으로부터 수집된 대량의 계좌, 고객, 담보, 
 *    환율, 대외 신용평가(KAP/NICE) 데이터를 CDM(Common Data Model) 규격으로 정제하여 적재합니다.
 *
 * 2. 결정론적 데모 데이터 시딩(Deterministic Demo Seeding):
 *    시분할/시계열 분석 및 대손충당금(IFRS 9) 단계 산출(Staging Stage 1/2/3) 테스트 시, 실행 시점 마다 random seed나 
 *    가변 일자에 따라 적재 결과가 달라지면 배치 결과 검증 및 UI 시시각각 분석 정합성을 보장할 수 없습니다.
 *    본 서비스는 고정된 기준일자(e.g., 2026-04-15) 및 일관된 픽스처(Fixture) 데이터를 멱등(Idempotent)하게 적재하여
 *    데모/로컬 프로파일 환경에서 언제나 재현 가능하고 검증 가능한 데모 시드 데이터 상태를 보장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountMartDemoFixtureService {

    public static final LocalDate DEFAULT_DEMO_BASE_DATE = LocalDate.of(2026, 4, 15);

    private final JdbcTemplate jdbcTemplate;

    /**
     * 기본 기준일(DEFAULT_DEMO_BASE_DATE) 기반 시드 데이터 생성을 수행합니다.
     */
    @Transactional
    public void seedDemoFixtureData() {
        seedDemoFixtureData(DEFAULT_DEMO_BASE_DATE);
    }

    /**
     * 지정된 기준일자(baseDate) 기준으로 ODS 원천 및 외화/KAP/담보/잔액 시계열 시드 데이터를 적재합니다.
     *
     * @param baseDate 배치 및 마트 분석용 기준일자
     */
    @Transactional
    public void seedDemoFixtureData(LocalDate baseDate) {
        log.info("🌱 [Demo Seed Fixture] 기준일자({}) 대상 H2 데모 시드 데이터 적재를 시작합니다.", baseDate);

        // 1. 기존 동일 데모 데이터 존재 여부 확인 (멱등성 보장)
        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ods_acc_ledger WHERE acc_no LIKE 'DEMO-ACC%'",
                Integer.class);

        if (existingCount != null && existingCount > 0) {
            log.info("ℹ️ [Demo Seed Fixture] 이미 기존 데모 계좌 데이터({}건)가 존재하므로 시딩을 건너뜁니다.", existingCount);
            return;
        }

        // 2. 계정과목 마스터 (ods_acc_mst) 시딩 (ods_product_mst 외래 키 참조 정합성 보장)
        insertAccountSubject("L001", "원화대출금 계정", "LOAN", "BS", true, "ENTITY01");
        insertAccountSubject("L003", "원화약정한도 계정", "OFF_BALANCE", "BS", true, "ENTITY02");
        insertAccountSubject("L004", "신용카드채권 계정", "CARD", "BS", true, "ENTITY03");
        insertAccountSubject("L005", "외화대출금 계정", "LOAN", "BS", true, "ENTITY04");

        // 3. 상품 마스터 (ods_product_mst) 시딩
        insertProduct("DEMO-LOAN", "대손충당금 대출", "LOAN", "L001", false, new BigDecimal("1.0000"));
        insertProduct("DEMO-LIMIT", "대손충당금 약정", "OFF_BALANCE", "L003", true, new BigDecimal("0.5000"));
        insertProduct("DEMO-CARD", "대손충당금 카드", "CARD", "L004", false, new BigDecimal("1.0000"));
        insertProduct("DEMO-USD", "대손충당금 외화대출", "LOAN", "L005", false, new BigDecimal("1.0000"));

        // 3. 고객 마스터 (ods_customer_mst) 시딩
        insertCustomer("DEMO-CUST001", "정상 개인고객", "RETAIL", "KR", "A", false, "BR001");
        insertCustomer("DEMO-CUST002", "주의 법인고객", "CORPORATE", "KR", "BBB", false, "BR001");
        insertCustomer("DEMO-CUST003", "한도 약정 SME", "SME", "KR", "BBB", true, "BR002");
        insertCustomer("DEMO-CUST004", "부도 카드차주", "RETAIL", "KR", "CCC", false, "BR003");
        insertCustomer("DEMO-CUST005", "외화대출 차주", "CORPORATE", "US", "A", false, "BR004");

        // 4. 여신 계정 원장 (ods_acc_ledger) 및 시계열 잔액 (ods_balance_hist) 시딩
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

        // Data Quality 오류 검증용 음수 잔액 테스트 계좌 1건
        jdbcTemplate.update("""
                INSERT INTO ods_acc_ledger (
                    acc_no, customer_code, prod_cd, currency, outstd_amt, limit_amt, int_rate,
                    base_rate_cd, spread, next_reset_dt, open_dt, maturity_dt, delinquent_days,
                    repayment_method, grace_period, repayment_freq, branch_cd, biz_unit_cd, is_active
                ) VALUES ('DEMO-ACC-DQ-ERR', 'DEMO-CUST001', 'DEMO-LOAN', 'KRW', -1000.0000, 1000000.0000, 4.500000, 'KORIBOR_3M', 1.000000, ?, ?, ?, 0, 'BULLET', 0, 1, 'BR001', 'ENTITY01', FALSE)
                """,
                Date.valueOf(baseDate.plusMonths(3)),
                Date.valueOf(baseDate.minusYears(1)),
                Date.valueOf(baseDate.plusYears(3)));

        // 5. 총계정원장 (ods_general_ledger) 시딩
        insertGeneralLedger(baseDate, "L001", "KRW", new BigDecimal("300000000.0000"), "BR001");
        insertGeneralLedger(baseDate, "L003", "KRW", new BigDecimal("100000000.0000"), "BR002");
        insertGeneralLedger(baseDate, "L004", "KRW", new BigDecimal("5000000.0000"), "BR003");
        insertGeneralLedger(baseDate, "L005", "USD", new BigDecimal("10000.0000"), "BR004");

        // 6. 매매기준 환율 (market_exchange_rate) 시딩
        jdbcTemplate.update("""
                INSERT INTO market_exchange_rate (
                    base_dt, base_currency, quote_currency, base_rate, bid_rate, ask_rate, source, created_at
                ) VALUES (?, 'USD', 'KRW', ?, ?, ?, 'DEMO_SEED', CURRENT_TIMESTAMP)
                """,
                Date.valueOf(baseDate),
                new BigDecimal("1350.0000"),
                new BigDecimal("1349.0000"),
                new BigDecimal("1351.0000"));

        // 7. KAP 외부 신용평가 등급 (kap_external_ratings) 시딩
        jdbcTemplate.update("""
                INSERT INTO kap_external_ratings (
                    customer_id, eval_agency, rating_grade, base_date
                ) VALUES 
                ('DEMO-CUST001', 'KAP', 'AAA', ?),
                ('DEMO-CUST002', 'KAP', 'BBB+', ?),
                ('DEMO-CUST005', 'KAP', 'AA', ?)
                """,
                Date.valueOf(baseDate),
                Date.valueOf(baseDate),
                Date.valueOf(baseDate));

        // 8. 담보 마스터 (ods_coll_mst) & 아파트 시세 (ods_apart_coll_detail) 시딩
        String collateralNo = "COLL-DEMO-001";
        jdbcTemplate.update("""
                INSERT INTO ods_coll_mst (
                    collateral_no, customer_code, collateral_type, currency, appraisal_amount, pledge_amount, appraisal_date, is_active
                ) VALUES (?, 'DEMO-CUST001', 'REAL_ESTATE', 'KRW', 150000000.0000, 120000000.0000, ?, TRUE)
                """,
                collateralNo,
                Date.valueOf(baseDate));

        jdbcTemplate.update("""
                INSERT INTO ods_apart_coll_detail (
                    coll_id, district_cd, kb_market_price, house_type, exclusive_area, floor_no, is_speculative_area
                ) VALUES (?, 'D0001', 160000000.0000, 'APARTMENT', 84.00, 10, FALSE)
                """,
                collateralNo);

        log.info("✅ [Demo Seed Fixture] H2 데모 시드 데이터 적재 완료 (계좌/원장/환율/KAP/담보)");
    }

    private void insertAccountSubject(String subjectCode,
                                      String subjectName,
                                      String accountType,
                                      String bsClass,
                                      boolean isAsset,
                                      String businessUnitCode) {
        jdbcTemplate.update("""
                INSERT INTO ods_acc_mst (
                    subj_cd, subj_nm, acc_type, bs_class, is_asset, biz_unit_cd, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                subjectCode,
                subjectName,
                accountType,
                bsClass,
                isAsset,
                businessUnitCode);
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
                "IND-DEMO",
                "데모 테스트 산업",
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
                Date.valueOf(baseDate.plusMonths(3)),
                Date.valueOf(baseDate.minusYears(1)),
                Date.valueOf(baseDate.plusYears(3)),
                delinquentDays,
                branchCode,
                businessUnitCode);

        jdbcTemplate.update("""
                INSERT INTO ods_balance_hist (base_dt, account_no, currency, balance)
                VALUES (?, ?, ?, ?)
                """,
                Date.valueOf(baseDate),
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
                Date.valueOf(baseDate),
                subjectCode,
                currency,
                balance,
                branchCode);
    }
}
