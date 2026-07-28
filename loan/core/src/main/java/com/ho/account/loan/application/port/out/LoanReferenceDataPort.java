package com.ho.account.loan.application.port.out;

import java.time.LocalDate;

/**
 * 대출에서 필요한 거래처·통화·계정 기준정보 확인 포트.
 */
public interface LoanReferenceDataPort {

    LoanReferenceSnapshot requireLoanReferences(
            Long businessPartnerId,
            String currencyCode,
            LocalDate effectiveDate);

    /**
     * 자동분개에 사용할 계정 코드가 실제 활성 계정인지 확인합니다.
     */
    AccountReference requireAccount(String accountCode, LocalDate effectiveDate);

    record LoanReferenceSnapshot(
            Long businessPartnerId,
            String businessPartnerName,
            String currencyCode) {

        public LoanReferenceSnapshot {
            if (businessPartnerId == null || businessPartnerId < 1) {
                throw new IllegalArgumentException("businessPartnerId must be positive.");
            }
            businessPartnerName = requireText(businessPartnerName, "businessPartnerName");
            currencyCode = normalizeCurrencyCode(currencyCode);
        }
    }

    record AccountReference(String code, String name) {
        public AccountReference {
            code = requireText(code, "accountCode");
            name = requireText(name, "accountName");
        }
    }

    private static String normalizeCurrencyCode(String value) {
        String normalized = requireText(value, "currencyCode").toUpperCase(java.util.Locale.ROOT);
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter ISO code.");
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }
}
