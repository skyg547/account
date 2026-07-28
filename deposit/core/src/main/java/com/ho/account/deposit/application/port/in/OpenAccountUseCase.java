package com.ho.account.deposit.application.port.in;

import java.math.BigDecimal;
import java.util.Locale;

public interface OpenAccountUseCase {
    String openAccount(OpenAccountCommand command);

    /**
     * 예금 계좌 개설 유즈케이스의 입력 command입니다.
     *
     * <p>초보자 가이드: API나 Batch 같은 바깥 계층은 이 command로 업무 요청을 전달합니다.
     * 필수 코드와 금액/금리의 기본 무결성은 여기서 먼저 막고, 잔액 증가 같은 상태 변경은 도메인 객체가 담당합니다.
     */
    record OpenAccountCommand(
            String customerCode,
            String productCode,
            String currencyCode,
            BigDecimal initialDeposit,
            BigDecimal interestRate
    ) {
        public OpenAccountCommand {
            customerCode = requireText(customerCode, "customerCode");
            productCode = requireText(productCode, "productCode");
            currencyCode = requireText(currencyCode, "currencyCode").toUpperCase(Locale.ROOT);
            initialDeposit = initialDeposit == null ? BigDecimal.ZERO : initialDeposit;
            if (initialDeposit.signum() < 0) {
                throw new IllegalArgumentException("initialDeposit must be zero or positive.");
            }
            if (interestRate == null || interestRate.signum() < 0) {
                throw new IllegalArgumentException("interestRate must be zero or positive.");
            }
        }

        private static String requireText(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required.");
            }
            return value.trim();
        }
    }
}