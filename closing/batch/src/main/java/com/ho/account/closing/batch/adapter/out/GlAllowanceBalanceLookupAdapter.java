package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GlAllowanceBalanceLookupAdapter implements AllowanceBalanceLookupPort {

    private static final String SELECT_LATEST_SIGNED_BALANCE = """
            SELECT ending_balance
              FROM gl_balances
             WHERE account_code = ?
               AND currency_code = ?
               AND balance_date <= ?
             ORDER BY balance_date DESC, period DESC
             FETCH FIRST 1 ROW ONLY
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public BigDecimal findCreditEndingBalance(String allowanceAccountCode, String currencyCode, LocalDate balanceDate) {
        List<BigDecimal> signedBalances = jdbcTemplate.query(
                SELECT_LATEST_SIGNED_BALANCE,
                (resultSet, rowNumber) -> resultSet.getBigDecimal("ending_balance"),
                allowanceAccountCode,
                currencyCode,
                Date.valueOf(balanceDate));
        if (signedBalances.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal signedDebitBalance = signedBalances.get(0);
        return signedDebitBalance == null ? BigDecimal.ZERO : signedDebitBalance.negate();
    }
}
