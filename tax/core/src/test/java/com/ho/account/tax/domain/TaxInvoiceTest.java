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

    @Test
    void allowsLosslessTrailingZerosAndMaximumIntegerDigits() {
        assertDoesNotThrow(() -> invoice("99999999999999999.990", "0.000", "99999999999999999.990"));
        assertDoesNotThrow(() -> invoice("1.000", "0", "1.000"));
    }

    @Test
    void rejectsAmountsThatNeedRoundingOrExceedColumnPrecision() {
        assertThrows(IllegalArgumentException.class, () -> invoice("1.005", "0", "1.005"));
        assertThrows(IllegalArgumentException.class, () -> invoice("1.005", "0.005", "1.010"));
        assertThrows(IllegalArgumentException.class, () -> invoice("100000000000000000", "0", "100000000000000000"));
        assertThrows(IllegalArgumentException.class, () -> invoice("0", "0.001", "0.001"));
    }

    @Test
    void rejectsInvalidUpdateBeforeChangingExistingValues() {
        TaxInvoice invoice = invoice("1.00", "0.10", "1.10");

        assertThrows(IllegalArgumentException.class, () -> invoice.updateInfo(
                "TX-CHANGED", LocalDate.of(2026, 5, 9), "BP002",
                new BigDecimal("1.005"), BigDecimal.ZERO, new BigDecimal("1.005")));
        assertEquals("TX-VALID", invoice.getIssueId());
        assertEquals(new BigDecimal("1.00"), invoice.getSupplyAmount());
        assertEquals(new BigDecimal("1.10"), invoice.getTotalAmount());

        assertDoesNotThrow(() -> invoice.updateInfo(
                "TX-CHANGED", LocalDate.of(2026, 5, 9), "BP002",
                new BigDecimal("1.000"), new BigDecimal("0.100"), new BigDecimal("1.100")));
    }

    private TaxInvoice invoice(String supply, String tax, String total) {
        return TaxInvoice.create("TX-VALID", "PURCHASE", LocalDate.of(2026, 5, 8), "BP001",
                new BigDecimal(supply), new BigDecimal(tax), new BigDecimal(total));
    }
}
