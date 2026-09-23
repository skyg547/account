package com.ho.account.cashflow.core.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashflowStatementTest {

    private static final LocalDateTime GENERATED_AT = LocalDateTime.of(2026, 9, 23, 9, 30);

    @ParameterizedTest
    @EnumSource(CashflowMethod.class)
    void aggregatesEveryActivityForDirectAndIndirectStatements(CashflowMethod method) {
        CashflowStatement statement = CashflowStatement.generate(
                "stmt-2026-09-" + method,
                2026,
                9,
                method,
                "krw",
                GENERATED_AT,
                List.of(
                        line("OP-01", CashflowActivity.OPERATING, "1000"),
                        line("OP-02", CashflowActivity.OPERATING, "-125.50"),
                        line("INV-01", CashflowActivity.INVESTING, "-300"),
                        line("FIN-01", CashflowActivity.FINANCING, "50")),
                new BigDecimal("500"));

        assertThat(statement.getMethod()).isEqualTo(method);
        assertThat(statement.getTotalOperating()).isEqualByComparingTo("874.50");
        assertThat(statement.getTotalInvesting()).isEqualByComparingTo("-300.00");
        assertThat(statement.getTotalFinancing()).isEqualByComparingTo("50.00");
        assertThat(statement.getNetCashflow()).isEqualByComparingTo("624.50");
        assertThat(statement.getBeginningCash()).isEqualByComparingTo("500.00");
        assertThat(statement.getEndingCash()).isEqualByComparingTo("1124.50");
        assertThat(statement.getEndingCash().scale()).isEqualTo(2);
    }

    @Test
    void rejectsPersistedSnapshotWhenNetCashflowDoesNotMatchActivityTotals() {
        List<CashflowLineItem> items = List.of(line("OP-01", CashflowActivity.OPERATING, "10"));

        assertThatThrownBy(() -> CashflowStatement.reconstitute(
                "stmt-invalid-net", 2026, 9, CashflowMethod.DIRECT, "KRW", GENERATED_AT, items,
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("9"), new BigDecimal("100"), new BigDecimal("109")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("netCashflow");
    }

    @Test
    void rejectsPersistedSnapshotWhenEndingCashDoesNotBalance() {
        List<CashflowLineItem> items = List.of(line("OP-01", CashflowActivity.OPERATING, "10"));

        assertThatThrownBy(() -> CashflowStatement.reconstitute(
                "stmt-invalid-ending", 2026, 9, CashflowMethod.INDIRECT, "KRW", GENERATED_AT, items,
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("109")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endingCash");
    }

    @Test
    void rejectsSilentRoundingAndMixedCurrencies() {
        assertThatThrownBy(() -> line("OP-01", CashflowActivity.OPERATING, "1.001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two decimal places");

        assertThatThrownBy(() -> CashflowStatement.generate(
                "stmt-mixed", 2026, 9, CashflowMethod.DIRECT, "KRW", GENERATED_AT,
                List.of(new CashflowLineItem(
                        "OP-USD", "receipt", CashflowActivity.OPERATING, BigDecimal.ONE, "USD", "mixed")),
                BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currencies");
    }

    private static CashflowLineItem line(String code, CashflowActivity activity, String amount) {
        return new CashflowLineItem(code, "test-category", activity, new BigDecimal(amount), "KRW", code);
    }
}
