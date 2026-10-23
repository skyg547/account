package com.ho.account.closing.domain;

/**
 * 한 법인과 회계연도에 대해 승인된 이익잉여금 대체 계정 통제입니다.
 *
 * <p>{@code postable}은 계정 마스터의 다른 속성에서 추론하지 않는 명시적인 통제면 증명입니다.</p>
 */
public record ApprovedRetainedEarningsMapping(
        String legalEntityCode,
        int fiscalYear,
        String accountCode,
        boolean postable,
        String approvedBy,
        String changeReference) {

    private static final int MAX_LEGAL_ENTITY_CODE_LENGTH = 20;
    private static final int MAX_ACCOUNT_CODE_LENGTH = 50;
    private static final int MAX_APPROVED_BY_LENGTH = 80;
    private static final int MAX_CHANGE_REFERENCE_LENGTH = 200;

    public ApprovedRetainedEarningsMapping {
        legalEntityCode = requireText(
                legalEntityCode, "legalEntityCode", MAX_LEGAL_ENTITY_CODE_LENGTH);
        if (fiscalYear < 1900 || fiscalYear > 9999) {
            throw new IllegalArgumentException("fiscalYear must be between 1900 and 9999");
        }
        accountCode = requireText(accountCode, "accountCode", MAX_ACCOUNT_CODE_LENGTH);
        if (!postable) {
            throw new IllegalArgumentException("postable control attestation must be true");
        }
        approvedBy = requireText(approvedBy, "approvedBy", MAX_APPROVED_BY_LENGTH);
        changeReference = requireText(
                changeReference, "changeReference", MAX_CHANGE_REFERENCE_LENGTH);
    }

    /**
     * 길이 접두어를 사용해 값 경계의 모호함 없이 스냅샷 해시 입력을 결정적으로 만듭니다.
     */
    public String controlIdentity() {
        return canonical(
                "APPROVED_RETAINED_EARNINGS_MAPPING_V1",
                legalEntityCode,
                Integer.toString(fiscalYear),
                accountCode,
                Boolean.toString(postable),
                approvedBy,
                changeReference);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private static String canonical(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            builder.append(value.length()).append(':').append(value);
        }
        return builder.toString();
    }
}
