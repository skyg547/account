package com.ho.account.masterdata.core.domain.policy;

import java.math.BigDecimal;

/** Storage-aligned business field limits shared by direct commands and approval payloads. */
public final class MasterDataFieldRules {
    public static final class FieldValidationException extends IllegalArgumentException {
        public FieldValidationException(String message) { super(message); }
    }
    public static final int ACCOUNT_CODE_MAX = 20;
    public static final int DEPARTMENT_CODE_MAX = 20;
    public static final int BUSINESS_PARTNER_CODE_MAX = 20;
    public static final int PRODUCT_CODE_MAX = 50;
    public static final int ACCOUNT_NAME_MAX = 100;
    public static final int DEPARTMENT_NAME_MAX = 100;
    public static final int BUSINESS_PARTNER_NAME_MAX = 100;
    public static final int PRODUCT_NAME_MAX = 200;
    public static final int ACCOUNT_REPORT_LINE_MAX = 100;
    public static final int ACCOUNT_REGULATORY_MAPPING_CODE_MAX = 100;
    public static final int PRODUCT_DESCRIPTION_MAX = 500;
    public static final int PRODUCT_UNIT_MAX = 20;
    public static final int PARTNER_REGISTRATION_MAX = 20;
    public static final int PARTNER_CEO_MAX = 50;
    public static final int PARTNER_BUSINESS_TYPE_MAX = 50;
    public static final int PARTNER_BUSINESS_ITEM_MAX = 50;
    public static final int PARTNER_ACCOUNT_TEXT_MAX = 100;
    public static final int PARTNER_SWIFT_MAX = 20;

    private MasterDataFieldRules() { }

    public static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new FieldValidationException(field + " is required.");
        }
        return bounded(value, field, maxLength);
    }

    public static String optionalText(String value, String field, int maxLength) {
        return value == null || value.isBlank() ? null : bounded(value, field, maxLength);
    }

    public static String code(String value, String field, int maxLength) {
        return requiredText(value, field, maxLength);
    }

    private static String bounded(String value, String field, int maxLength) {
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new FieldValidationException(field + " must be at most " + maxLength + " characters.");
        }
        return normalized;
    }

    public static BigDecimal productPrice(BigDecimal price) {
        BigDecimal value = price == null ? BigDecimal.ZERO : price;
        // NUMERIC(19,4): avoid database rounding or overflow after an approval is recorded.
        if (value.stripTrailingZeros().scale() > 4 || value.precision() - value.scale() > 15) {
            throw new FieldValidationException("Product price must fit NUMERIC(19,4).");
        }
        return value;
    }
}
