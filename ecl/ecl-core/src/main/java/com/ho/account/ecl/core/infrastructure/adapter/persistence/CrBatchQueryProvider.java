package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.JPAExpressions;
import com.ho.account.ecl.core.domain.exposure.QCrAccount;
import com.ho.account.ecl.core.domain.result.QAllowanceEclResult;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.ecl.core.domain.exposure.QCrCustomer;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.QAllowanceExposureSnapshotReadEntity;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * [Infrastructure] QueryDSL 기반 대손충당금(IFRS9) 배치 산출용 쿼리 공급자.
 */
public class CrBatchQueryProvider {

    /**
     * [Phase 1] 필수 baseDate와 파티션 ID 범위에서 Stage/PD 산출 대상 계좌를 조회한다.
     * DQ에서 비활성화한 계좌는 제외하고, 같은 기준일 snapshot의 source_account_no와
     * 일치하는 계좌만 EXISTS 조건으로 반환한다. snapshot에 없는 계좌는 산출하지 않으며, 같은
     * baseDate로 재실행해도 해당 날짜 snapshot을 기준으로 대상 계좌를 다시 선택한다.
     */
    public static BiFunction<JPAQueryFactory, LongRange, JPAQuery<CrAccount>> accountPagingQuery(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "baseDate is required for Stage account selection");
        QCrAccount account = QCrAccount.crAccount;
        QAllowanceExposureSnapshotReadEntity snapshot = QAllowanceExposureSnapshotReadEntity.allowanceExposureSnapshotReadEntity;
        return (queryFactory, range) -> queryFactory
                .selectFrom(account)
                .join(account.customer).fetchJoin()
                .where(account.isActive.isTrue(),
                       account.id.goe(range.min()),
                       account.id.loe(range.max()),
                       // EXISTS keeps one account per page even if the snapshot has several exposures for it.
                       JPAExpressions.selectOne().from(snapshot)
                               .where(snapshot.baseDate.eq(baseDate),
                                      snapshot.sourceAccountNo.eq(account.accountNo))
                               .exists())
                .orderBy(account.id.asc());
    }

    /**
     * [Phase 2~4] EAD/LGD/ECL 업데이트용 결과 레코드 조회 쿼리 생성 함수 (기준일 파라미터 적용).
     */
    public static BiFunction<JPAQueryFactory, LongRange, JPAQuery<AllowanceEclResult>> resultPagingQuery(java.time.LocalDate baseDate) {
        return (queryFactory, range) -> {
            com.querydsl.core.types.dsl.BooleanExpression predicate = QAllowanceEclResult.allowanceEclResult.id.goe(range.min())
                    .and(QAllowanceEclResult.allowanceEclResult.id.loe(range.max()));
            if (baseDate != null) {
                predicate = predicate.and(QAllowanceEclResult.allowanceEclResult.baseDate.eq(baseDate));
            }
            return queryFactory
                    .selectFrom(QAllowanceEclResult.allowanceEclResult)
                    .join(QAllowanceEclResult.allowanceEclResult.account).fetchJoin()
                    .join(QAllowanceEclResult.allowanceEclResult.account.customer).fetchJoin()
                    .where(predicate)
                    .orderBy(QAllowanceEclResult.allowanceEclResult.id.asc());
        };
    }

    public static BiFunction<JPAQueryFactory, LongRange, JPAQuery<AllowanceEclResult>> resultPagingQuery() {
        return resultPagingQuery(null);
    }

    /**
     * [Phase 2] 담보 배분 최적화를 위한 고객(Customer) 조회 쿼리 생성 함수
     */
    public static BiFunction<JPAQueryFactory, LongRange, JPAQuery<CrCustomer>> customerPagingQuery() {
        return (queryFactory, range) -> queryFactory
                .selectFrom(QCrCustomer.crCustomer)
                .where(QCrCustomer.crCustomer.id.goe(range.min()),
                       QCrCustomer.crCustomer.id.loe(range.max()))
                .orderBy(QCrCustomer.crCustomer.id.asc());
    }

    // =========================================================================
    // [Bulk SQL] 대용량 처리를 위한 고성능 원시 SQL (Adapters 참조용)
    // =========================================================================

    /** [Account] 연체 일수 기반 스테이지 일괄 업데이트 */
    public static final String UPDATE_STAGING_BY_DAYS_SQL = """
            UPDATE cr_accounts 
            SET staging = :targetStage, updated_at = CURRENT_TIMESTAMP 
            WHERE delinquent_days >= :lowerDays %s AND is_active = true
            """;

    /** [Account] 조기경보 레벨 기반 스테이지 일괄 업데이트 */
    public static final String UPDATE_STAGING_BY_WARNING_SQL = """
            UPDATE cr_accounts
            SET staging = :targetStage, updated_at = CURRENT_TIMESTAMP
            WHERE customer_id IN (SELECT id FROM cr_customers WHERE warning_level = :warningLevel)
              AND staging = 'STAGE1' AND is_active = true
            """;

    /** [Account] 데이터 품질 오류 마킹 (Identity 정보 누락) */
    public static final String MARK_INVALID_IDENTITY_SQL = """
            UPDATE cr_accounts SET error_message = 'CR001: Missing Identity Info', is_active = false
            WHERE (account_no IS NULL OR customer_id IS NULL) AND is_active = true
            """;

    /** [Account] 데이터 품질 오류 마킹 (필수 산출 파라미터 누락) */
    public static final String MARK_INVALID_MODEL_PARAM_SQL = """
            UPDATE cr_accounts SET error_message = 'ALW002: Missing Allowance Model Params', is_active = false
            WHERE customer_id IN (SELECT id FROM cr_customers WHERE internal_rating IS NULL OR customer_type IS NULL)
              AND is_active = true
            """;

    /** [Account] 데이터 품질 오류 마킹 (비정상 금액) */
    public static final String MARK_INVALID_AMOUNT_SQL = """
            UPDATE cr_accounts SET error_message = 'CR003: Invalid Amount', is_active = false
            WHERE (outstanding_amt < 0 OR notional_amt < 0) AND is_active = true
            """;

    /** [Account] 오류 메시지 초기화 */
    public static final String CLEAR_ACCOUNT_ERROR_SQL = "UPDATE cr_accounts SET error_message = NULL WHERE is_active = true";

    /** [Account] 데이터 마트 동기화용 대량 Upsert (PostgreSQL ON CONFLICT) */
    public static final String UPSERT_ACCOUNT_FROM_CDM_SQL = """
            INSERT INTO cr_accounts (
                account_no, customer_id, product_code, currency, notional_amt, outstanding_amt,
                prod_category, int_rate, repayment_method, grace_period, repayment_freq,
                branch_cd, biz_unit_cd, staging, delinquent_days, open_date, maturity_date,
                internal_rating, is_debt_restructured, is_active, created_at, updated_at
            ) VALUES (
                :accountNo, :customerId, :productCode, :currency, :notionalAmount, :outstandingAmount,
                :productCategory, :interestRate, :repaymentMethod, :gracePeriod, :repaymentFreq,
                :branchCode, :bizUnitCode, :staging, :delinquentDays, :openDate, :maturityDate,
                :internalRating, :isDebtRestructured, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            ) ON CONFLICT (account_no) DO UPDATE SET
                customer_id = EXCLUDED.customer_id,
                product_code = EXCLUDED.product_code,
                currency = EXCLUDED.currency,
                notional_amt = EXCLUDED.notional_amt,
                outstanding_amt = EXCLUDED.outstanding_amt,
                prod_category = EXCLUDED.prod_category,
                int_rate = EXCLUDED.int_rate,
                repayment_method = EXCLUDED.repayment_method,
                grace_period = EXCLUDED.grace_period,
                repayment_freq = EXCLUDED.repayment_freq,
                branch_cd = EXCLUDED.branch_cd,
                biz_unit_cd = EXCLUDED.biz_unit_cd,
                staging = EXCLUDED.staging,
                delinquent_days = EXCLUDED.delinquent_days,
                maturity_date = EXCLUDED.maturity_date,
                internal_rating = EXCLUDED.internal_rating,
                is_debt_restructured = EXCLUDED.is_debt_restructured,
                updated_at = CURRENT_TIMESTAMP
            """;

    /** [Customer] 데이터 마트 동기화용 대량 Upsert (PostgreSQL ON CONFLICT) */
    public static final String UPSERT_CUSTOMER_FROM_CDM_SQL = """
            INSERT INTO cr_customers (
                customer_code, customer_name, customer_type, internal_rating, external_rating,
                industry_code, country_code, is_sme, warning_level, is_active, created_at, updated_at
            ) VALUES (
                :customerCode, :customerName, :customerType, :internalRating, :externalRating,
                :industryCode, :countryCode, :isSme, :warningLevel, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            ) ON CONFLICT (customer_code) DO UPDATE SET
                customer_name = EXCLUDED.customer_name,
                customer_type = EXCLUDED.customer_type,
                internal_rating = EXCLUDED.internal_rating,
                external_rating = EXCLUDED.external_rating,
                industry_code = EXCLUDED.industry_code,
                country_code = EXCLUDED.country_code,
                is_sme = EXCLUDED.is_sme,
                warning_level = EXCLUDED.warning_level,
                updated_at = CURRENT_TIMESTAMP
            """;

    /** [Collateral] 계좌-담보 매핑 테이블 초기화 */
    public static final String TRUNCATE_ACCOUNT_COLLATERAL_SQL = "TRUNCATE TABLE cr_account_collaterals";

    /** [Collateral] 차주 ID 기반 대출-담보 자동 매핑 생성 */
    public static final String CREATE_ACCOUNT_COLLATERAL_MAPPING_SQL = """
            INSERT INTO cr_account_collaterals (account_id, collateral_id, allocation_amount, priority, created_at, updated_at)
            SELECT a.id, c.id, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM cr_accounts a
            INNER JOIN cr_collaterals c ON a.customer_id = c.customer_id
            WHERE a.is_active = true AND c.is_active = true
            """;

    /** 파티셔닝 ID 범위를 위한 데이터 객체 (Java 14+ Record) */
    public record LongRange(long min, long max) {}

    private CrBatchQueryProvider() {
    }
}
