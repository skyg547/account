package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningDecision;
import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningSubmission;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerScreeningUseCase;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerScreeningPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;
import com.ho.account.masterdata.core.domain.policy.BusinessPartnerScreeningPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Serializes screening changes by stable business key and atomically records each state change. */
@Service
@Transactional
public class BusinessPartnerScreeningService implements BusinessPartnerScreeningUseCase {

    private final BusinessPartnerPersistencePort partners;
    private final BusinessPartnerScreeningPort screenings;
    private final MasterDataBusinessKeyLockPort businessKeyLock;

    public BusinessPartnerScreeningService(BusinessPartnerPersistencePort partners,
            BusinessPartnerScreeningPort screenings, MasterDataBusinessKeyLockPort businessKeyLock) {
        this.partners = partners;
        this.screenings = screenings;
        this.businessKeyLock = businessKeyLock;
    }

    /** A new submission replaces any previous decision with PENDING and requires a new review. */
    @Override
    public BusinessPartnerScreening submitScreening(BusinessPartnerScreeningSubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Screening submission is required.");
        }
        String code = requiredCode(submission.businessPartnerCode());
        businessKeyLock.lock(MasterDataType.BUSINESS_PARTNER, code);
        BusinessPartner partner = currentPartnerForUpdate(code);
        requireSupportedPartnerType(partner);
        BusinessPartnerScreening current = screenings.findByBusinessPartnerCodeForUpdate(code).orElse(null);
        if (current != null && current.status() == BusinessPartner.KycStatus.PENDING) {
            // Validate a replay against its original submission time so later expiry does not create another audit.
            BusinessPartnerScreening replay = candidate(code, submission, current.submittedAt());
            requireMatchingIdentifier(replay.identifier(), partner);
            if (sameSubmission(current, replay)) {
                return current;
            }
        }
        // A changed payload is a new submission and must have evidence valid today.
        BusinessPartnerScreening next = candidate(code, submission, LocalDateTime.now());
        requireMatchingIdentifier(next.identifier(), partner);
        // A failed current-row write or audit append rolls both back; retries of the same pending payload are no-ops.
        BusinessPartnerScreening saved = screenings.save(next);
        screenings.appendAudit(saved);
        return saved;
    }

    /** Only a different authenticated reviewer can decide a pending submission. */
    @Override
    public BusinessPartnerScreening decideScreening(BusinessPartnerScreeningDecision decision) {
        if (decision == null) {
            throw new IllegalArgumentException("Screening decision is required.");
        }
        String code = requiredCode(decision.businessPartnerCode());
        businessKeyLock.lock(MasterDataType.BUSINESS_PARTNER, code);
        BusinessPartner partner = currentPartnerForUpdate(code);
        requireSupportedPartnerType(partner);
        BusinessPartnerScreening current = screenings.findByBusinessPartnerCodeForUpdate(code)
                .orElseThrow(() -> new IllegalStateException("Screening submission is required before review."));
        requireMatchingIdentifier(current.identifier(), partner);
        if (current.status() != BusinessPartner.KycStatus.PENDING) {
            if (sameDecision(current, decision)) {
                return current;
            }
            throw new IllegalStateException("Screening has already been decided; submit new evidence first.");
        }
        BusinessPartnerScreening next = current.decide(decision.status(), decision.riskRating(),
                decision.decisionReason(), decision.reviewedBy(), LocalDateTime.now());
        BusinessPartnerScreening saved = screenings.save(next);
        screenings.appendAudit(saved);
        return saved;
    }

    /** Absence, unsupported type, inactive SCD2 state, and stale evidence all fail closed. */
    @Override
    @Transactional(readOnly = true)
    public boolean isEligible(String businessPartnerCode, LocalDate asOfDate) {
        if (businessPartnerCode == null || businessPartnerCode.isBlank() || asOfDate == null) {
            return false;
        }
        // Persistence uses an exclusive next-day bound; reject dates outside the supported business window first.
        if (asOfDate.getYear() < 1 || asOfDate.isAfter(BusinessPartner.OPEN_ENDED_VALID_TO)) {
            return false;
        }
        String code = businessPartnerCode.trim();
        return partners.findEffectiveByBusinessPartnerCode(code, asOfDate)
                .filter(partner -> partner.isActiveAt(asOfDate) && supportsScreening(partner))
                // Replay the audit state at the requested date; today's current row could be a later resubmission.
                .flatMap(partner -> screenings.findLatestAt(code, asOfDate)
                        .filter(screening -> screening.isEligible(asOfDate)
                                && BusinessPartnerScreeningPolicy.matchesPartnerRegistration(
                                        screening.identifier(), partner.getRegistrationNumber())))
                .isPresent();
    }

    private BusinessPartner currentPartnerForUpdate(String code) {
        BusinessPartner partner = partners.findByBusinessPartnerCodeForUpdate(code)
                .orElseThrow(() -> new NoSuchElementException("Business partner not found."));
        if (!partner.isActiveAt(LocalDate.now())) {
            throw new IllegalStateException("Only an active partner can be screened.");
        }
        return partner;
    }

    private static boolean supportsScreening(BusinessPartner partner) {
        return partner.getPartnerType() == BusinessPartner.PartnerType.CUSTOMER
                || partner.getPartnerType() == BusinessPartner.PartnerType.VENDOR;
    }

    private static void requireSupportedPartnerType(BusinessPartner partner) {
        if (!supportsScreening(partner)) {
            throw new IllegalArgumentException("Identifier policy is unsupported for this partner type.");
        }
    }

    private static void requireMatchingIdentifier(String identifier, BusinessPartner partner) {
        if (!BusinessPartnerScreeningPolicy.matchesPartnerRegistration(identifier,
                partner.getRegistrationNumber())) {
            throw new IllegalArgumentException("Screened identifier must match the partner registration number.");
        }
    }

    private static String requiredCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Business partner code is required.");
        }
        return code.trim();
    }

    private static BusinessPartnerScreening candidate(String code,
            BusinessPartnerScreeningSubmission submission, LocalDateTime submittedAt) {
        return BusinessPartnerScreening.submit(code, submission.jurisdiction(), submission.identifierType(),
                submission.identifier(), submission.evidenceReference(), submission.evidenceExpiresOn(),
                submission.submittedBy(), submittedAt);
    }

    private static boolean sameSubmission(BusinessPartnerScreening current,
            BusinessPartnerScreening requested) {
        return Objects.equals(current.jurisdiction(), requested.jurisdiction())
                && Objects.equals(current.identifierType(), requested.identifierType())
                && Objects.equals(current.identifier(), requested.identifier())
                && Objects.equals(current.evidenceReference(), requested.evidenceReference())
                && Objects.equals(current.evidenceExpiresOn(), requested.evidenceExpiresOn())
                && Objects.equals(current.submittedBy(), requested.submittedBy());
    }

    private static boolean sameDecision(BusinessPartnerScreening current,
            BusinessPartnerScreeningDecision requested) {
        return current.status() == requested.status()
                && current.riskRating() == requested.riskRating()
                && Objects.equals(current.decisionReason(), requested.decisionReason())
                && Objects.equals(current.reviewedBy(), requested.reviewedBy());
    }
}
