package com.ho.account.tax.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class TaxInvoiceTest {

    @Test
    @DisplayName("공급가액과 세액의 합이 합계금액과 일치하면 검증을 통과한다")
    void validateAmountSuccess() {
        assertDoesNotThrow(() -> TaxInvoice.create(
                "TX-VALID",
                "PURCHASE",
                LocalDate.of(2026, 5, 8),
                "BP001",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1100.00")));
    }

    @Test
    @DisplayName("공급가액과 세액의 합이 합계금액과 일치하지 않으면 예외가 발생한다")
    void validateAmountFailure() {
        assertThrows(IllegalStateException.class, () -> TaxInvoice.create(
                "TX-INVALID",
                "PURCHASE",
                LocalDate.of(2026, 5, 8),
                "BP001",
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1200.00")));
    }
}
