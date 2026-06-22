package com.ho.account.expenditure.application.port.out;

import java.math.BigDecimal;

/**
 * 은행 또는 지급 대행 시스템에 실제 지급을 요청하는 출력 포트입니다.
 *
 * <p>지급 서비스는 은행 API의 구현 방식을 알지 않고, 동일한 멱등 키로 재요청하면
 * 같은 지급 결과를 돌려받는다는 업무 계약에만 의존합니다.</p>
 */
public interface PaymentExecutionPort {

    PaymentExecutionResult execute(PaymentExecutionCommand command);

    record PaymentExecutionCommand(
            String idempotencyKey,
            Long paymentId,
            Long payableId,
            String vendorCode,
            BigDecimal amount,
            String bankAccount) {
    }

    record PaymentExecutionResult(boolean successful, String referenceNo, String failureReason) {
        public static PaymentExecutionResult completed(String referenceNo) {
            return new PaymentExecutionResult(true, referenceNo, null);
        }

        public static PaymentExecutionResult failed(String failureReason) {
            return new PaymentExecutionResult(false, null, failureReason);
        }
    }
}
