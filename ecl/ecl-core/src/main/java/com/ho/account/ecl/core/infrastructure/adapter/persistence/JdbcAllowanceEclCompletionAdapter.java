package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.AllowanceEclCompletionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class JdbcAllowanceEclCompletionAdapter implements AllowanceEclCompletionPort {

    private static final String CALCULATED_ECL_PREDICATE = """
            FROM cr_risk_results
           WHERE base_date = ?
             AND COALESCE(weighted_ecl, expected_loss) IS NOT NULL
            """;

    private static final String COUNT_CALCULATED_ECL_RESULTS = "SELECT COUNT(*) " + CALCULATED_ECL_PREDICATE;

    private static final String MARK_CALCULATED_ECL_COMPLETED = """
            UPDATE cr_risk_results
               SET status = 'COMPLETED',
                   calculation_completed_at = CURRENT_TIMESTAMP
             WHERE base_date = ?
               AND COALESCE(weighted_ecl, expected_loss) IS NOT NULL
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int countCalculatedEclResults(LocalDate baseDate) {
        Integer count = jdbcTemplate.queryForObject(
                COUNT_CALCULATED_ECL_RESULTS,
                Integer.class,
                Date.valueOf(baseDate));
        return count == null ? 0 : count;
    }

    @Override
    public int markCalculatedEclResultsCompleted(LocalDate baseDate) {
        return jdbcTemplate.update(MARK_CALCULATED_ECL_COMPLETED, Date.valueOf(baseDate));
    }
}
