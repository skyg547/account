package com.ho.account.closing.batch.adapter.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GlAllowanceBalanceLookupAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private GlAllowanceBalanceLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:closing-ecl-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        adapter = new GlAllowanceBalanceLookupAdapter(jdbcTemplate);
        jdbcTemplate.execute("""
                CREATE TABLE gl_balances (
                    account_code VARCHAR(50) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    balance_date DATE NOT NULL,
                    period VARCHAR(7) NOT NULL,
                    ending_balance DECIMAL(19,2)
                )
                """);
    }

    @Test
    void convertsDebitMinusCreditSignedBalanceToPositiveCreditBalance() {
        jdbcTemplate.update(
                "INSERT INTO gl_balances VALUES ('129100', 'USD', DATE '2026-04-30', '2026-04', -700.00)");
        jdbcTemplate.update(
                "INSERT INTO gl_balances VALUES ('129100', 'USD', DATE '2026-05-31', '2026-05', -800.00)");

        assertThat(adapter.findCreditEndingBalance(
                "129100", "USD", LocalDate.of(2026, 5, 31)))
                .isEqualByComparingTo("800.00");
    }
}
