package com.ho.account.asset.application.service;

import java.util.Map;

/** Business identities are independent of generated database IDs and retry-time values. */
final class AssetJournalIdentity {
    private AssetJournalIdentity() { }

    static void fixedAsset(Map<String, Object> event, String code, String operation) {
        identify(event, "FIXED_ASSET_CODE", code, operation);
    }

    static void lease(Map<String, Object> event, String contractNo, String operation) {
        identify(event, "IFRS16_LEASE_CONTRACT_NO", contractNo, operation);
    }

    private static void identify(Map<String, Object> event, String type, String source, String operation) {
        event.put("lineageSourceType", type);
        event.put("lineageSourceId", requireSourceCode(source));
        event.put("journalOperationKey", operation);
    }

    static String requireSourceCode(String code) {
        if (code == null || code.isBlank() || code.length() > 100 || !code.equals(code.trim())) {
            throw new IllegalArgumentException("asset code/contract number must be nonblank, trimmed and at most 100 characters");
        }
        return code;
    }

    static String requireOperationId(String operationId) {
        if (operationId == null || !operationId.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}")) {
            throw new IllegalArgumentException("remeasurement operationId must be 1-80 letters, digits, dots, underscores, colons or hyphens");
        }
        return operationId;
    }
}
