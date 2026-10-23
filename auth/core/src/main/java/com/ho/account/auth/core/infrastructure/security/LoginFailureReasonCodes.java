package com.ho.account.auth.core.infrastructure.security;

import java.util.Set;

/** Restricts persisted and logged failure reasons to application-owned, credential-free codes. */
final class LoginFailureReasonCodes {
    private static final Set<String> ALLOWED = Set.of(
            "INACTIVE_ACCOUNT", "ADMINISTRATIVELY_LOCKED", "INVALID_DEPARTMENT",
            "UNSUPPORTED_DATA_SCOPE", "NO_EFFECTIVE_ROLE", "MISSING_PASSWORD",
            "INVALID_PASSWORD", "USER_NOT_FOUND", "SSO_AUTHENTICATION_FAILED", "SSO_INVALID_SUBJECT",
            "SSO_SUBJECT_MISMATCH", "SSO_UNMAPPED_USER", "OTP_VERIFICATION_FAILED",
            "OTP_REQUIRED", "INVALID_OTP");

    private LoginFailureReasonCodes() {}

    static String normalize(String reason) {
        return reason != null && ALLOWED.contains(reason) ? reason : "UNKNOWN";
    }
}
