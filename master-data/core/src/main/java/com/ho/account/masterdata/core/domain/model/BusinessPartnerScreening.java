package com.ho.account.masterdata.core.domain.model;

import com.ho.account.masterdata.core.domain.policy.BusinessPartnerScreeningPolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Current screening snapshot keyed by the stable partner code, independent of SCD2 row IDs. */
public record BusinessPartnerScreening(
        String businessPartnerCode,
        String jurisdiction,
        String identifierType,
        String identifier,
        String evidenceReference,
        LocalDate evidenceExpiresOn,
        BusinessPartner.KycStatus status,
        BusinessPartner.RiskRating riskRating,
        String submittedBy,
        LocalDateTime submittedAt,
        String reviewedBy,
        String decisionReason,
        LocalDateTime decidedAt) {

    public BusinessPartnerScreening {
        businessPartnerCode = required(businessPartnerCode, 20, "Partner code");
        jurisdiction = required(jurisdiction, 2, "Jurisdiction");
        identifierType = required(identifierType, 64, "Identifier type");
        identifier = required(identifier, 128, "Identifier");
        evidenceReference = required(evidenceReference, 512, "Evidence reference");
        submittedBy = actor(submittedBy);
        if (submittedAt == null || status == null || riskRating == null) {
            throw new IllegalArgumentException("Submission time, status, and risk rating are required.");
        }
        BusinessPartnerScreeningPolicy.requireSupportedIdentifier(jurisdiction, identifierType, identifier);
        if (evidenceExpiresOn != null && evidenceExpiresOn.isBefore(submittedAt.toLocalDate())) {
            throw new IllegalArgumentException("Evidence must not already be expired on submission.");
        }
        if (status == BusinessPartner.KycStatus.PENDING) {
            if (riskRating != BusinessPartner.RiskRating.LOW || reviewedBy != null
                    || decisionReason != null || decidedAt != null) {
                throw new IllegalArgumentException("Pending screening cannot carry a decision.");
            }
        } else {
            reviewedBy = actor(reviewedBy);
            decisionReason = required(decisionReason, 1000, "Decision reason");
            if (decidedAt == null) {
                throw new IllegalArgumentException("Decision time is required.");
            }
            if (submittedBy.equals(reviewedBy) || decidedAt.isBefore(submittedAt)) {
                throw new IllegalArgumentException("Decision requires a distinct reviewer after submission.");
            }
        }
    }

    public static BusinessPartnerScreening submit(String code, String jurisdiction, String identifierType,
            String identifier, String evidenceReference, LocalDate evidenceExpiresOn,
            String submittedBy, LocalDateTime submittedAt) {
        return new BusinessPartnerScreening(code, jurisdiction, identifierType, identifier,
                evidenceReference, evidenceExpiresOn, BusinessPartner.KycStatus.PENDING,
                BusinessPartner.RiskRating.LOW, submittedBy, submittedAt, null, null, null);
    }

    public BusinessPartnerScreening decide(BusinessPartner.KycStatus decision,
            BusinessPartner.RiskRating risk, String reason, String reviewer, LocalDateTime at) {
        if (status != BusinessPartner.KycStatus.PENDING) {
            throw new IllegalStateException("Only pending screening can be decided.");
        }
        if (decision == null || decision == BusinessPartner.KycStatus.PENDING) {
            throw new IllegalArgumentException("Decision must be APPROVED, REJECTED, or REVIEW_REQUIRED.");
        }
        // A reviewer cannot approve evidence that has already expired at the decision time.
        if (at == null) {
            throw new IllegalArgumentException("Decision time is required.");
        }
        if (risk == null) {
            throw new IllegalArgumentException("Risk rating is required.");
        }
        if (decision == BusinessPartner.KycStatus.APPROVED
                && !BusinessPartnerScreeningPolicy.hasCurrentEvidence(evidenceReference,
                        evidenceExpiresOn, at.toLocalDate())) {
            throw new IllegalStateException("Expired evidence cannot be approved.");
        }
        return new BusinessPartnerScreening(businessPartnerCode, jurisdiction, identifierType,
                identifier, evidenceReference, evidenceExpiresOn, decision,
                risk, submittedBy,
                submittedAt, reviewer, reason, at);
    }

    /** An approved decision alone never makes an inactive or unsupported partner eligible. */
    public boolean isEligible(LocalDate asOfDate) {
        return asOfDate != null
                && status == BusinessPartner.KycStatus.APPROVED
                && decidedAt != null && !asOfDate.isBefore(decidedAt.toLocalDate())
                && (riskRating == BusinessPartner.RiskRating.LOW
                    || riskRating == BusinessPartner.RiskRating.MEDIUM)
                && BusinessPartnerScreeningPolicy.hasCurrentEvidence(evidenceReference, evidenceExpiresOn, asOfDate)
                && BusinessPartnerScreeningPolicy.isValidatedIdentifier(jurisdiction, identifierType, identifier);
    }

    private static String actor(String value) {
        String actor = required(value, 128, "Authenticated actor");
        if ("SYSTEM".equalsIgnoreCase(actor)) {
            throw new IllegalArgumentException("A human authenticated actor is required.");
        }
        return actor;
    }

    private static String required(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(field + " is required and must be at most " + max + " characters.");
        }
        return value.trim();
    }
}
