package com.ho.account.auth.core.domain.model;

import java.time.LocalDate;

/** Business constraints shared by receipt validation and decision revalidation; no transport or persistence types. */
public final class GovernanceChangePolicy {
    private GovernanceChangePolicy() {}

    public static void supportedChange(String masterType, String requestType) {
        text(masterType, 50);
        boolean supported = switch (masterType) {
            case "AUTH_USER_ROLE" -> "CREATE".equals(requestType) || "UPDATE".equals(requestType);
            case "SYSTEM_ROLE" -> "CREATE".equals(requestType);
            case "AUTHORIZATION" -> "CREATE".equals(requestType) || "DELETE".equals(requestType);
            default -> false;
        };
        require(supported);
    }

    public static void roleDefinition(String masterKey, String roleCode, String roleName, String description) {
        require(text(masterKey, 100).equals(text(roleCode, 50)));
        text(roleName, 100);
        optional(description, 500);
    }

    public static void authorization(String masterKey, String roleCode, String functionCode, String accessType) {
        text(roleCode, 50);
        text(functionCode, 100);
        text(accessType, 20);
        require(text(masterKey, 100).equals(roleCode + ":" + functionCode + ":" + accessType));
    }

    public static LocalDate effectiveDate(LocalDate requested, LocalDate today) {
        require(today != null);
        return requested == null ? today : requested;
    }

    public static int requestedVersion(Integer requested) {
        int version = requested == null ? 1 : requested;
        require(version > 0);
        return version;
    }

    public static String scope(String value) {
        require(value == null || "GLOBAL".equals(value));
        return "GLOBAL";
    }

    public static String text(String value, int max) {
        require(value != null && !value.isBlank() && value.length() <= max && value.equals(value.trim()));
        return value;
    }

    public static void optional(String value, int max) { require(value == null || value.length() <= max); }
    public static void require(boolean valid) {
        if (!valid) throw new IllegalArgumentException("Invalid governance request");
    }
}
