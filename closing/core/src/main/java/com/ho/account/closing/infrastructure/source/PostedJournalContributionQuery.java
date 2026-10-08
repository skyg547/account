package com.ho.account.closing.infrastructure.source;

/**
 * Shared SQL for reconstructing transaction- and functional-currency balances from posted journals.
 *
 * <p>The recursive relation attributes reporting-currency FX adjustments and every reversal descendant
 * back to the original {@code batchId|accountCode|sourceCurrency} lineage. Ordinary journal rows retain
 * their own transaction currency. Every consumer applies the same POSTED/cutoff and lineage checks so
 * API preflight and Batch cursor execution cannot silently disagree about financial evidence.</p>
 */
public final class PostedJournalContributionQuery {

    private static final String CONTRIBUTIONS = """
            WITH RECURSIVE fx_lineage_tokens AS (
                SELECT e.id, e.entry_type, e.currency_code, e.lineage_source_id,
                       CASE WHEN POSITION('|' IN e.lineage_source_id) > 0
                            THEN SUBSTRING(e.lineage_source_id FROM 1
                                           FOR POSITION('|' IN e.lineage_source_id) - 1)
                            ELSE '' END AS batch_id,
                       CASE WHEN POSITION('|' IN e.lineage_source_id) > 0
                            THEN SUBSTRING(e.lineage_source_id
                                           FROM POSITION('|' IN e.lineage_source_id) + 1)
                            ELSE '' END AS account_and_currency
                  FROM journal_entries e
                 WHERE e.lineage_source_type = 'FX_VALUATION'
            ), fx_roots AS (
                SELECT t.*,
                       CASE WHEN POSITION('|' IN t.account_and_currency) > 0
                            THEN SUBSTRING(t.account_and_currency FROM 1
                                           FOR POSITION('|' IN t.account_and_currency) - 1)
                            ELSE '' END AS account_code,
                       UPPER(CASE WHEN POSITION('|' IN t.account_and_currency) > 0
                                  THEN SUBSTRING(t.account_and_currency
                                                 FROM POSITION('|' IN t.account_and_currency) + 1)
                                  ELSE '' END) AS source_currency
                  FROM fx_lineage_tokens t
            ), fx_entries (entry_id, account_code, source_currency, reporting_currency, invalid_lineage) AS (
                SELECT r.id, CAST(r.account_code AS VARCHAR(50)),
                       CAST(r.source_currency AS VARCHAR(3)), UPPER(r.currency_code),
                       CASE WHEN r.entry_type = 'CLOSING_ADJUSTMENT'
                                  AND REGEXP_REPLACE(r.lineage_source_id,
                                      '^[1-9][0-9]*[|][^|]+[|][A-Za-z]{3}$', '') = ''
                                  AND LENGTH(r.batch_id) <= 19
                                  AND (LENGTH(r.batch_id) < 19 OR r.batch_id <= '9223372036854775807')
                                  AND LENGTH(r.account_code) BETWEEN 1 AND 50
                                  AND r.account_code = TRIM(r.account_code)
                                  AND r.source_currency <> UPPER(r.currency_code)
                            THEN 0 ELSE 1 END
                  FROM fx_roots r
                UNION ALL
                SELECT reversal.id, parent.account_code, parent.source_currency, parent.reporting_currency,
                       CASE WHEN UPPER(reversal.currency_code) = parent.reporting_currency
                            THEN parent.invalid_lineage ELSE 1 END
                  FROM journal_entries reversal
                  JOIN fx_entries parent
                    ON reversal.lineage_source_id = CAST(parent.entry_id AS VARCHAR)
                 WHERE reversal.entry_type = 'REVERSAL'
                   AND reversal.lineage_source_type = 'JOURNAL_ENTRY'
            ), eligible_contributions AS (
                SELECT CASE WHEN fx.entry_id IS NULL THEN d.account_code ELSE fx.account_code END AS account_code,
                       CASE WHEN fx.entry_id IS NULL THEN UPPER(e.currency_code)
                            ELSE fx.source_currency END AS currency_code,
                       CASE WHEN fx.entry_id IS NOT NULL THEN CAST(0 AS NUMERIC(19, 2))
                            WHEN d.side = 'DEBIT' THEN d.amount ELSE -d.amount END AS foreign_amount,
                       CASE WHEN d.side = 'DEBIT' THEN d.base_amount ELSE -d.base_amount END AS base_amount,
                       CASE WHEN fx.entry_id IS NOT NULL
                                  AND (fx.invalid_lineage <> 0 OR d.id IS NULL)
                            THEN 1 ELSE 0 END AS invalid_lineage,
                       e.accounting_date, fx.entry_id AS fx_entry_id, fx.reporting_currency AS fx_reporting_currency
                  FROM journal_entries e
                  LEFT JOIN fx_entries fx ON fx.entry_id = e.id
                  LEFT JOIN journal_details d
                    ON d.journal_entry_id = e.id
                   AND (fx.entry_id IS NULL OR d.account_code = fx.account_code)
                 WHERE e.status = 'POSTED'
                   AND (fx.entry_id IS NOT NULL OR d.id IS NOT NULL)
            )
            """;

    // H2's recursive CTE binding loses parameters inside WITH; bind only in the final SELECT.
    private static final String ELIGIBLE_FROM = """
              FROM eligible_contributions c
              CROSS JOIN (SELECT CAST(? AS DATE) AS valuation_date,
                                 CAST(? AS VARCHAR(3)) AS reporting_currency) p
             WHERE c.accounting_date <= p.valuation_date
               AND (c.fx_entry_id IS NOT NULL OR c.currency_code <> p.reporting_currency)
            """;

    private static final String INVALID_LINEAGE = """
            CASE WHEN c.fx_entry_id IS NOT NULL AND c.fx_reporting_currency <> p.reporting_currency
                 THEN 1 ELSE c.invalid_lineage END
            """;

    public static final String ACCOUNT_COUNT_SQL = CONTRIBUTIONS + """
            SELECT COUNT(DISTINCT account_code) AS account_count,
                   COALESCE(MAX(%s), 0) AS invalid_lineage
            """.formatted(INVALID_LINEAGE) + ELIGIBLE_FROM;

    public static final String ORDERED_ACCOUNTS_SQL = CONTRIBUTIONS + """
            SELECT DISTINCT account_code
            """ + ELIGIBLE_FROM + " ORDER BY account_code";

    public static final String BALANCES_SQL = CONTRIBUTIONS + """
            SELECT account_code, currency_code,
                   SUM(foreign_amount) AS foreign_ending_balance,
                   SUM(base_amount) AS book_reporting_amount,
                   MAX(%s) AS invalid_lineage
            """.formatted(INVALID_LINEAGE) + ELIGIBLE_FROM + """
             GROUP BY account_code, currency_code
            HAVING SUM(foreign_amount) <> 0 OR SUM(base_amount) <> 0 OR MAX(%s) <> 0
             ORDER BY account_code, currency_code
            """.formatted(INVALID_LINEAGE);

    public static final String PARTITION_BALANCES_SQL = CONTRIBUTIONS + """
            SELECT account_code, currency_code,
                   SUM(foreign_amount) AS foreign_ending_balance,
                   SUM(base_amount) AS book_reporting_amount,
                   MAX(%s) AS invalid_lineage
            """.formatted(INVALID_LINEAGE) + ELIGIBLE_FROM + """
               AND (c.invalid_lineage <> 0 OR c.fx_reporting_currency <> p.reporting_currency
                    OR (c.account_code >= ? AND c.account_code <= ?))
             GROUP BY account_code, currency_code
            HAVING SUM(foreign_amount) <> 0 OR SUM(base_amount) <> 0 OR MAX(%s) <> 0
             ORDER BY account_code, currency_code
            """.formatted(INVALID_LINEAGE);

    // ECL includes functional-currency rows and is independent of dated FX eligibility policy.
    public static final String ALLOWANCE_BALANCE_SQL = CONTRIBUTIONS + """
            SELECT COALESCE(SUM(c.foreign_amount), 0) AS transaction_amount,
                   COALESCE(SUM(c.base_amount), 0) AS base_amount,
                   COALESCE(MAX(%s), 0) AS invalid_lineage
              FROM eligible_contributions c
              CROSS JOIN (SELECT CAST(? AS DATE) AS valuation_date,
                                 CAST(? AS VARCHAR(3)) AS reporting_currency) p
             WHERE c.accounting_date <= p.valuation_date
               AND (c.invalid_lineage <> 0 OR c.fx_reporting_currency <> p.reporting_currency
                    OR (c.account_code = ? AND c.currency_code = ?))
            """.formatted(INVALID_LINEAGE);

    private PostedJournalContributionQuery() {
    }

    public static void requireValidLineage(int invalidLineage) {
        if (invalidLineage != 0) {
            throw new IllegalStateException("FX valuation lineage is invalid or its account leg is missing");
        }
    }
}
