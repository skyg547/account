package com.ho.account.reconciliation.application.port.out;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public record ExternalReconSnapshotRequest(
        String unitId,
        String stageCode,
        LocalDate reconciliationDate,
        String productCode,
        String currencyCode,
        String legalEntityCode
) {

    public static final String SOURCE_STAGE = "SOURCE";
    public static final String INTERFACE_STAGE = "INTERFACE";

    public ExternalReconSnapshotRequest {
        unitId = requiredText(unitId, "unitId");
        stageCode = requiredText(stageCode, "stageCode").toUpperCase(Locale.ROOT);
        if (reconciliationDate == null) {
            throw new IllegalArgumentException("reconciliationDate must not be null");
        }
        productCode = defaultString(productCode);
        currencyCode = defaultString(currencyCode);
        legalEntityCode = defaultString(legalEntityCode);
    }

    public static ExternalReconSnapshotRequest of(
            String unitId,
            String stageCode,
            LocalDate reconciliationDate,
            String productCode,
            String currencyCode,
            String legalEntityCode
    ) {
        return new ExternalReconSnapshotRequest(
                unitId,
                stageCode,
                reconciliationDate,
                productCode,
                currencyCode,
                legalEntityCode
        );
    }

    private static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String defaultString(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
