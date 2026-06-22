package com.ho.account.expenditure.adapter.out.execution;

import com.ho.account.expenditure.application.port.out.PaymentExecutionPort.PaymentExecutionCommand;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPaymentExecutionAdapterTest {

    @Test
    void returnsSameReferenceForSameIdempotencyKey() {
        LocalPaymentExecutionAdapter adapter = new LocalPaymentExecutionAdapter();
        PaymentExecutionCommand command = new PaymentExecutionCommand(
                "PAYMENT:10", 10L, 100L, "V001", new BigDecimal("100.00"), "BANK-001");

        String first = adapter.execute(command).referenceNo();
        String retry = adapter.execute(command).referenceNo();

        assertThat(retry).isEqualTo(first);
    }
}
