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
}
