package com.ho.account.masterdata.core.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BusinessPartnerScreeningPolicyTest {

    private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime DECIDED_AT = SUBMITTED_AT.plusHours(1);
    private static final LocalDate EXPIRY = LocalDate.of(2026, 10, 31);

    @ParameterizedTest
    @CsvSource({
            "APPROVED, LOW, true",
            "APPROVED, MEDIUM, true",
            "APPROVED, HIGH, false",
            "APPROVED, CRITICAL, false",
            "REJECTED, LOW, false",
            "REVIEW_REQUIRED, LOW, false"
    })
    void eligibilityRequiresApprovedDecisionAndAcceptableRisk(
            BusinessPartner.KycStatus status, BusinessPartner.RiskRating risk, boolean eligible) {
        BusinessPartnerScreening screening = pending().decide(status, risk, "Evidence checked", "reviewer", DECIDED_AT);

        assertThat(screening.isEligible(EXPIRY)).isEqualTo(eligible);
        assertThat(screening.isEligible(EXPIRY.plusDays(1))).isFalse();
        assertThat(screening.reviewedBy()).isEqualTo("reviewer");
        assertThat(screening.decisionReason()).isEqualTo("Evidence checked");
        assertThat(screening.decidedAt()).isEqualTo(DECIDED_AT);
        assertThat(screening.evidenceReference()).isEqualTo("synthetic-evidence-923");
    }

    @Test
    void pendingIsIneligibleAndOnlyPendingCanBeDecided() {
        assertThat(pending().isEligible(EXPIRY)).isFalse();
        BusinessPartnerScreening decided = pending().decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW, "Checked", "reviewer", DECIDED_AT);

        assertThatIllegalStateException().isThrownBy(() -> decided.decide(
                BusinessPartner.KycStatus.REJECTED, BusinessPartner.RiskRating.HIGH,
                "Changed", "other-reviewer", DECIDED_AT.plusMinutes(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> pending().decide(
                BusinessPartner.KycStatus.PENDING, BusinessPartner.RiskRating.LOW,
                "No decision", "reviewer", DECIDED_AT));
    }

    @Test
    void submitterCannotReviewOwnEvidence() {
        assertThatIllegalArgumentException().isThrownBy(() -> pending().decide(
                BusinessPartner.KycStatus.APPROVED, BusinessPartner.RiskRating.LOW,
                "Self review", "submitter", DECIDED_AT));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "KR|BUSINESS_REGISTRATION|123456789|false",
            "KR|BUSINESS_REGISTRATION|12345678901|false",
            "KR|BUSINESS_REGISTRATION|12345A7890|false",
            "KR|BUSINESS_REGISTRATION|１２３４５６７８９０|false",
            "US|BUSINESS_REGISTRATION|1234567890|false",
            "KR|TAX_ID|1234567890|false",
            "KR|BUSINESS_REGISTRATION|1234567890|true"
    }, delimiter = '|')
    void identifierPolicyIsExplicitAndFailsClosed(String jurisdiction, String type, String identifier, boolean valid) {
        assertThat(BusinessPartnerScreeningPolicy.isValidatedIdentifier(jurisdiction, type, identifier))
                .isEqualTo(valid);
    }

    @Test
    void missingIdentifierAndExpiredSubmissionAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> BusinessPartnerScreening.submit(
                "BP-923", "KR", "BUSINESS_REGISTRATION", " ",
                "synthetic-evidence-923", EXPIRY, "submitter", SUBMITTED_AT));
        assertThatIllegalArgumentException().isThrownBy(() -> BusinessPartnerScreening.submit(
                "BP-923", "KR", "BUSINESS_REGISTRATION", "1234567890",
                "synthetic-evidence-923", SUBMITTED_AT.toLocalDate().minusDays(1), "submitter", SUBMITTED_AT));
    }

    @Test
    void registrationMustMatchTheScreenedIdentifierAfterStrictNormalization() {
        assertThat(BusinessPartnerScreeningPolicy.matchesPartnerRegistration(
                "1234567890", "123-45-67890")).isTrue();
        assertThat(BusinessPartnerScreeningPolicy.matchesPartnerRegistration(
                "1234567890", "1234567890")).isTrue();
        assertThat(BusinessPartnerScreeningPolicy.matchesPartnerRegistration(
                "1234567890", "987-65-43210")).isFalse();
        assertThat(BusinessPartnerScreeningPolicy.matchesPartnerRegistration(
                "1234567890", "123 45 67890")).isFalse();
        assertThat(BusinessPartnerScreeningPolicy.matchesPartnerRegistration(
                "1234567890", null)).isFalse();
    }

    private BusinessPartnerScreening pending() {
        return BusinessPartnerScreening.submit("BP-923", "KR", "BUSINESS_REGISTRATION",
                "1234567890", "synthetic-evidence-923", EXPIRY, "submitter", SUBMITTED_AT);
    }
}
