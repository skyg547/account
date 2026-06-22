package com.ho.account.receivable.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ReceivableTest {

    @Test
    @DisplayName("수납 금액을 적용하면 잔액이 차감되고 상태가 업데이트된다")
    void applyCollection() {
        // given
        Receivable receivable = new Receivable();
        receivable.setOriginalAmount(new BigDecimal("5000.00"));
        receivable.setOutstandingAmount(new BigDecimal("5000.00"));
        receivable.setStatus(ReceivableStatus.OPEN);

        // when
        receivable.applyCollection(new BigDecimal("2000.00"));

        // then
        assertEquals(new BigDecimal("3000.00"), receivable.getOutstandingAmount());
        assertEquals(ReceivableStatus.PARTIAL_PAID, receivable.getStatus());
    }

    @Test
    @DisplayName("전액 수납 시 상태가 PAID로 변경된다")
    void fullCollection() {
        // given
        Receivable receivable = new Receivable();
        receivable.setOutstandingAmount(new BigDecimal("5000.00"));

        // when
        receivable.applyCollection(new BigDecimal("5000.00"));

        // then
        assertEquals(BigDecimal.ZERO, receivable.getOutstandingAmount());
        assertEquals(ReceivableStatus.PAID, receivable.getStatus());
    }
}
