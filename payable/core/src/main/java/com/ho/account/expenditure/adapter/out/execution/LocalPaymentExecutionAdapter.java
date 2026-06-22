package com.ho.account.expenditure.adapter.out.execution;

import com.ho.account.expenditure.application.port.out.PaymentExecutionPort;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * 로컬 실행용 지급 어댑터입니다.
 *
 * <p>🐣 운영에서는 이 자리를 은행 API 어댑터로 교체합니다. 로컬 어댑터도 멱등 키별 결과를
 * 보관하므로 같은 지급을 재시도해도 새로운 송금 참조번호를 만들지 않습니다.</p>
 */
@Component
public class LocalPaymentExecutionAdapter implements PaymentExecutionPort {

    private final ConcurrentMap<String, PaymentExecutionResult> results = new ConcurrentHashMap<>();

    @Override
    public PaymentExecutionResult execute(PaymentExecutionCommand command) {
        return results.computeIfAbsent(command.idempotencyKey(), ignored -> executeOnce(command));
    }

    private PaymentExecutionResult executeOnce(PaymentExecutionCommand command) {
        if (command.bankAccount() == null || command.bankAccount().isBlank()) {
            return PaymentExecutionResult.failed("Bank account is required");
        }
        String referenceNo = "LOCAL-" + UUID.nameUUIDFromBytes(
                command.idempotencyKey().getBytes(StandardCharsets.UTF_8));
        return PaymentExecutionResult.completed(referenceNo);
    }
}
