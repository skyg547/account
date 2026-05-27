package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.AllowanceExposureSyncPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class JdbcAllowanceExposureSyncAdapter implements AllowanceExposureSyncPort {

    private static final String COUNT_SOURCE_SNAPSHOTS = """
            SELECT COUNT(*)
              FROM allowance_exposure_snapshots
             WHERE base_date = ?
            """;

    private static final String UPSERT_CUSTOMERS_FROM_SNAPSHOT = """
            WITH ranked_customers AS (
                SELECT customer_code,
                       customer_type,
                       is_sme,
                       country_code,
                       industry_code,
                       current_rating,
                       warning_level,
                       ROW_NUMBER() OVER (
                           PARTITION BY customer_code
                           ORDER BY exposure_id
                       ) AS row_number
                  FROM allowance_exposure_snapshots
                 WHERE base_date = ?
            )
            INSERT INTO cr_customers (
                   customer_code,
                   customer_name,
                   customer_type,
                   internal_rating,
                   industry_code,
                   country_code,
                   is_sme,
                   warning_level,
                   is_active,
                   created_at,
                   updated_at
            )
            SELECT customer_code,
                   'Allowance-' || customer_code,
                   COALESCE(NULLIF(customer_type, ''), 'CORPORATE'),
                   current_rating,
                   industry_code,
                   COALESCE(NULLIF(country_code, ''), 'KR'),
                   COALESCE(is_sme, FALSE),
                   COALESCE(NULLIF(warning_level, ''), 'NORMAL'),
                   TRUE,
                   CURRENT_TIMESTAMP,
                   CURRENT_TIMESTAMP
              FROM ranked_customers
             WHERE row_number = 1
            ON CONFLICT (customer_code) DO UPDATE SET
                   customer_type = EXCLUDED.customer_type,
                   internal_rating = EXCLUDED.internal_rating,
                   industry_code = EXCLUDED.industry_code,
                   country_code = EXCLUDED.country_code,
                   is_sme = EXCLUDED.is_sme,
                   warning_level = EXCLUDED.warning_level,
                   is_active = TRUE,
                   updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_ACCOUNTS_FROM_SNAPSHOT = """
            INSERT INTO cr_accounts (
                   account_no,
                   customer_id,
                   product_code,
                   currency,
                   notional_amt,
                   outstanding_amt,
                   prod_category,
                   int_rate,
                   branch_cd,
                   biz_unit_cd,
                   staging,
                   delinquent_days,
                   open_date,
                   maturity_date,
                   original_rating,
                   internal_rating,
                   is_debt_restructured,
                   is_active,
                   created_at,
                   updated_at
            )
            SELECT s.source_account_no,
                   c.id,
                   s.product_code,
                   COALESCE(NULLIF(s.currency_code, ''), 'KRW'),
                   COALESCE(s.outstanding_amount, 0) + COALESCE(s.undrawn_amount, 0),
                   COALESCE(s.outstanding_amount, 0),
                   s.product_category,
                   s.interest_rate,
                   s.branch_code,
                   s.legal_entity_code,
                   COALESCE(NULLIF(s.staging, ''), 'STAGE1'),
                   COALESCE(s.delinquent_days, 0),
                   COALESCE(s.open_date, s.base_date),
                   s.maturity_date,
                   s.original_rating,
                   s.current_rating,
                   COALESCE(s.debt_restructured, FALSE),
                   TRUE,
                   CURRENT_TIMESTAMP,
                   CURRENT_TIMESTAMP
              FROM allowance_exposure_snapshots s
              JOIN cr_customers c
                ON c.customer_code = s.customer_code
             WHERE s.base_date = ?
            ON CONFLICT (account_no) DO UPDATE SET
                   customer_id = EXCLUDED.customer_id,
                   product_code = EXCLUDED.product_code,
                   currency = EXCLUDED.currency,
                   notional_amt = EXCLUDED.notional_amt,
                   outstanding_amt = EXCLUDED.outstanding_amt,
                   prod_category = EXCLUDED.prod_category,
                   int_rate = EXCLUDED.int_rate,
                   branch_cd = EXCLUDED.branch_cd,
                   biz_unit_cd = EXCLUDED.biz_unit_cd,
                   staging = EXCLUDED.staging,
                   delinquent_days = EXCLUDED.delinquent_days,
                   open_date = EXCLUDED.open_date,
                   maturity_date = EXCLUDED.maturity_date,
                   original_rating = EXCLUDED.original_rating,
                   internal_rating = EXCLUDED.internal_rating,
                   is_debt_restructured = EXCLUDED.is_debt_restructured,
                   is_active = TRUE,
                   updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int countSourceSnapshots(LocalDate baseDate) {
        Integer count = jdbcTemplate.queryForObject(COUNT_SOURCE_SNAPSHOTS, Integer.class, Date.valueOf(baseDate));
        return count == null ? 0 : count;
    }

    @Override
    public int upsertCustomersFromSnapshot(LocalDate baseDate) {
        return jdbcTemplate.update(UPSERT_CUSTOMERS_FROM_SNAPSHOT, Date.valueOf(baseDate));
    }

    @Override
    public int upsertAccountsFromSnapshot(LocalDate baseDate) {
        return jdbcTemplate.update(UPSERT_ACCOUNTS_FROM_SNAPSHOT, Date.valueOf(baseDate));
    }
}
