package com.ho.account.expenditure.application.port.in;

/**
 * 개별 지급 실행 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: paymentId는 이미 생성된 지급 후보를 가리키고, bankAccount는 실제 출금 계좌입니다.
 * 외부 송금 어댑터는 이 값으로 멱등 키와 지급 참조번호를 관리합니다.</p>
 */
public record ExecutePaymentCommand(Long paymentId, String bankAccount) {
    public ExecutePaymentCommand {
        if (paymentId == null || paymentId <= 0) {
            throw new IllegalArgumentException("paymentId must be greater than zero");
        }
        if (bankAccount == null || bankAccount.isBlank()) {
            throw new IllegalArgumentException("bankAccount is required");
        }
        bankAccount = bankAccount.trim();
    }
}