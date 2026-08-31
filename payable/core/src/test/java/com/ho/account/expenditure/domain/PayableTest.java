package com.ho.account.expenditure.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PayableTest {

    @Test
    @DisplayName("지급 금액을 적용하면 잔액이 차감되고 상태가 PARTIAL_PAID로 바뀐다")
    void applyPartialPayment() {
        // given
        Payable payable = new Payable();
        payable.setOriginalAmount(new BigDecimal("1000.00"));
        payable.setOutstandingAmount(new BigDecimal("1000.00"));
        payable.setStatus(PayableStatus.OPEN);

        // when
        payable.applyPayment(new BigDecimal("400.00"));

        // then
        assertEquals(new BigDecimal("600.00"), payable.getOutstandingAmount());
        assertEquals(PayableStatus.PARTIAL_PAID, payable.getStatus());
    }

    @Test
    @DisplayName("전액 지급 시 잔액이 0이 되고 상태가 PAID로 바뀐다")
    void applyFullPayment() {
        // given
        Payable payable = new Payable();
        payable.setOriginalAmount(new BigDecimal("1000.00"));
        payable.setOutstandingAmount(new BigDecimal("1000.00"));
        payable.setStatus(PayableStatus.OPEN);

        // when
        payable.applyPayment(new BigDecimal("1000.00"));

        // then
        assertEquals(BigDecimal.ZERO, payable.getOutstandingAmount());
        assertEquals(PayableStatus.PAID, payable.getStatus());
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 지급하려 하면 예외가 발생한다")
    void applyOverPayment() {
        // given
        Payable payable = new Payable();
        payable.setOutstandingAmount(new BigDecimal("1000.00"));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            payable.applyPayment(new BigDecimal("1100.00"));
        });
    }

    @Test
    @DisplayName("지급 적격 상태(OPEN, APPROVED, UNPAID, PARTIAL_PAID, OVERDUE) 검증")
    void isEligibleForPayment() {
        Payable payable = new Payable();
        payable.setOriginalAmount(new BigDecimal("1000.00"));
        payable.setOutstandingAmount(new BigDecimal("1000.00"));

        payable.setStatus(PayableStatus.OPEN);
        assertTrue(payable.isEligibleForPayment());

        payable.setStatus(PayableStatus.APPROVED);
        assertTrue(payable.isEligibleForPayment());

        payable.setStatus(PayableStatus.UNPAID);
        assertTrue(payable.isEligibleForPayment());

        payable.setStatus(PayableStatus.PARTIAL_PAID);
        assertTrue(payable.isEligibleForPayment());

        payable.setStatus(PayableStatus.OVERDUE);
        assertTrue(payable.isEligibleForPayment());

        // 비적격 상태들
        payable.setStatus(PayableStatus.IN_PAYMENT);
        assertFalse(payable.isEligibleForPayment());

        payable.setStatus(PayableStatus.PAID);
        assertFalse(payable.isEligibleForPayment());

        payable.setStatus(PayableStatus.WRITTEN_OFF);
        assertFalse(payable.isEligibleForPayment());

        payable.setStatus(PayableStatus.OPEN);
        payable.setOutstandingAmount(BigDecimal.ZERO);
        assertFalse(payable.isEligibleForPayment());
    }

    @Test
    @DisplayName("markAsInPayment는 적격 채무를 IN_PAYMENT로 전이하고, 비적격 채무는 예외를 던진다")
    void markAsInPaymentTransitionsOrThrows() {
        Payable payable = new Payable();
        payable.setOriginalAmount(new BigDecimal("1000.00"));
        payable.setOutstandingAmount(new BigDecimal("1000.00"));
        payable.setStatus(PayableStatus.OPEN);

        payable.markAsInPayment();
        assertEquals(PayableStatus.IN_PAYMENT, payable.getStatus());

        // 이미 IN_PAYMENT 상태이면 다시 호출 시 예외 발생
        assertThrows(IllegalStateException.class, payable::markAsInPayment);
    }

    @Test
    @DisplayName("releaseFromPayment는 IN_PAYMENT 상태를 잔액에 맞게 해제한다")
    void releaseFromPayment() {
        Payable payable = new Payable();
        payable.setOriginalAmount(new BigDecimal("1000.00"));
        payable.setOutstandingAmount(new BigDecimal("1000.00"));
        payable.setStatus(PayableStatus.IN_PAYMENT);

        payable.releaseFromPayment();
        assertEquals(PayableStatus.OPEN, payable.getStatus());

        payable.setOutstandingAmount(new BigDecimal("600.00"));
        payable.setStatus(PayableStatus.IN_PAYMENT);
        payable.releaseFromPayment();
        assertEquals(PayableStatus.PARTIAL_PAID, payable.getStatus());
    }
}
