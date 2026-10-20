package com.ho.account.masterdata.core.domain.policy;

import java.time.LocalDate;

/**
 * Screening identifier policy. A format match does not establish that an identifier was issued;
 * approval still requires independent evidence and a distinct human reviewer.
 */
public final class BusinessPartnerScreeningPolicy {

    public static final String KR = "KR";
    public static final String BUSINESS_REGISTRATION = "BUSINESS_REGISTRATION";

    private BusinessPartnerScreeningPolicy() {
    }

    /** Only the declared KR business-registration format is currently supported. */
    public static boolean isValidatedIdentifier(String jurisdiction, String identifierType, String identifier) {
        return KR.equals(jurisdiction)
                && BUSINESS_REGISTRATION.equals(identifierType)
                && identifier != null
                && identifier.matches("[0-9]{10}");
    }

    public static void requireSupportedIdentifier(String jurisdiction, String identifierType, String identifier) {
        if (!isValidatedIdentifier(jurisdiction, identifierType, identifier)) {
            throw new IllegalArgumentException("Unsupported or invalid business partner identifier.");
        }
    }

    /** An edited partner registration invalidates a prior approval without rewriting its audit history. */
    public static boolean matchesPartnerRegistration(String identifier, String registrationNumber) {
        if (identifier == null || registrationNumber == null) {
            return false;
        }
        String normalized = registrationNumber.matches("[0-9]{10}")
                ? registrationNumber
                : registrationNumber.matches("[0-9]{3}-[0-9]{2}-[0-9]{5}")
                    ? registrationNumber.replace("-", "") : null;
        return identifier.equals(normalized);
    }

    public static boolean hasCurrentEvidence(String evidenceReference, LocalDate evidenceExpiresOn,
            LocalDate asOfDate) {
        return evidenceReference != null && !evidenceReference.isBlank()
                && asOfDate != null
                && (evidenceExpiresOn == null || !asOfDate.isAfter(evidenceExpiresOn));
    }
}
