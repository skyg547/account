package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.AllowanceExposureSnapshotBuildPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class JdbcAllowanceExposureSnapshotPersistenceAdapter implements AllowanceExposureSnapshotBuildPort {

    private static final String COUNT_SOURCE_POSITIONS = """
            SELECT COUNT(*)
              FROM dim_integrated_position_master
             WHERE base_dt = ?
            """;

    private static final String DELETE_BY_BASE_DATE = """
            DELETE FROM allowance_exposure_snapshots
             WHERE base_date = ?
            """;

    private static final String INSERT_FROM_INTEGRATED_POSITIONS = """
            INSERT INTO allowance_exposure_snapshots (
                   base_date,
                   exposure_id,
                   source_system,
                   source_account_no,
                   customer_code,
                   customer_type,
                   is_sme,
                   country_code,
                   industry_code,
                   product_code,
                   product_category,
                   legal_entity_code,
                   branch_code,
                   currency_code,
                   outstanding_amount,
                   undrawn_amount,
                   interest_rate,
                   effective_interest_rate,
                   open_date,
                   maturity_date,
                   delinquent_days,
                   staging,
                   original_rating,
                   current_rating,
                   warning_level,
                   debt_restructured,
                   collateral_value,
                   collateral_type,
                   accounting_account_code,
                   allowance_account_code,
                   created_at,
                   updated_at
            )
            SELECT p.base_dt,
                   p.acc_no,
                   'ACCOUNT_MART',
                   p.acc_no,
                   p.customer_code,
                   p.cust_type,
                   COALESCE(p.is_sme, FALSE),
                   p.country_cd,
                   p.industry_cd,
                   p.prod_cd,
                   p.prod_category,
                   COALESCE(NULLIF(p.biz_unit_cd, ''), 'DEFAULT'),
                   p.branch_cd,
                   COALESCE(NULLIF(p.currency, ''), 'KRW'),
                   COALESCE(p.outstd_amt, 0),
                   CASE
                       WHEN COALESCE(p.limit_amt, 0) > COALESCE(p.outstd_amt, 0)
                       THEN COALESCE(p.limit_amt, 0) - COALESCE(p.outstd_amt, 0)
                       ELSE 0
                   END,
                   p.int_rate,
                   p.int_rate,
                   p.open_dt,
                   p.maturity_dt,
                   COALESCE(p.delinquent_days, 0),
                   p.staging,
                   p.internal_rating,
                   p.internal_rating,
                   p.warning_level,
                   COALESCE(p.is_debt_restructured, FALSE),
                   COALESCE(p.recognized_coll_amt, p.coll_amt, 0),
                   p.coll_type,
                   product.subj_cd,
                   NULL,
                   CURRENT_TIMESTAMP,
                   CURRENT_TIMESTAMP
              FROM dim_integrated_position_master p
              LEFT JOIN ods_product_mst product
                ON product.prod_cd = p.prod_cd
             WHERE p.base_dt = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int countSourcePositions(LocalDate baseDate) {
        Integer count = jdbcTemplate.queryForObject(COUNT_SOURCE_POSITIONS, Integer.class, Date.valueOf(baseDate));
        return count == null ? 0 : count;
    }

    @Override
    public void deleteByBaseDate(LocalDate baseDate) {
        jdbcTemplate.update(DELETE_BY_BASE_DATE, Date.valueOf(baseDate));
    }

    @Override
    public int insertFromIntegratedPositions(LocalDate baseDate) {
        return jdbcTemplate.update(INSERT_FROM_INTEGRATED_POSITIONS, Date.valueOf(baseDate));
    }
}
