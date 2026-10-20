package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningDecision;
import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningSubmission;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerScreeningPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BusinessPartnerScreeningServiceTest {

    private BusinessPartnerPersistencePort partners;
    private BusinessPartnerScreeningPort screenings;
    private MasterDataBusinessKeyLockPort businessKeyLock;
    private BusinessPartnerScreeningService service;

    @BeforeEach
    void setUp() {
        partners = mock(BusinessPartnerPersistencePort.class);
        screenings = mock(BusinessPartnerScreeningPort.class);
        businessKeyLock = mock(MasterDataBusinessKeyLockPort.class);
        service = new BusinessPartnerScreeningService(partners, screenings, businessKeyLock);
    }

    @Test
    void submissionAndDecisionRecordCompleteAuditSnapshots() {
        when(partners.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        when(screenings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(screenings.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.empty())
                .thenAnswer(invocation -> Optional.of(pending()));

        BusinessPartnerScreening submitted = service.submitScreening(submission());
        BusinessPartnerScreening decided = service.decideScreening(decision());

        assertThat(submitted.status()).isEqualTo(BusinessPartner.KycStatus.PENDING);
        assertThat(decided.status()).isEqualTo(BusinessPartner.KycStatus.APPROVED);
        assertThat(decided.riskRating()).isEqualTo(BusinessPartner.RiskRating.MEDIUM);
        assertThat(decided.evidenceReference()).isEqualTo("synthetic-evidence-923");
        assertThat(decided.reviewedBy()).isEqualTo("reviewer");
        assertThat(decided.decidedAt()).isNotNull();
        ArgumentCaptor<BusinessPartnerScreening> audits = ArgumentCaptor.forClass(BusinessPartnerScreening.class);
        verify(screenings, times(2)).appendAudit(audits.capture());
        assertThat(audits.getAllValues()).extracting(BusinessPartnerScreening::status)
                .containsExactly(BusinessPartner.KycStatus.PENDING, BusinessPartner.KycStatus.APPROVED);
    }

    @Test
    void identicalPendingSubmissionAndIdenticalDecisionRetryDoNotWriteAgain() {
        when(partners.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        BusinessPartnerScreening pending = pending();
        BusinessPartnerScreening approved = pending.decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.MEDIUM, "Evidence checked", "reviewer", LocalDateTime.now());
        when(screenings.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(pending), Optional.of(approved));

        assertThat(service.submitScreening(submission())).isSameAs(pending);
        assertThat(service.decideScreening(decision())).isSameAs(approved);
        verify(screenings, never()).save(any());
        verify(screenings, never()).appendAudit(any());
    }

    @Test
    void paddedPendingSubmissionRetryUsesNormalizedFieldsWithoutAnotherAudit() {
        when(partners.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        BusinessPartnerScreening pending = pending();
        when(screenings.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.empty(), Optional.of(pending));
        when(screenings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BusinessPartnerScreeningSubmission padded = new BusinessPartnerScreeningSubmission(
                " BP-923 ", "KR", " BUSINESS_REGISTRATION ", " 1234567890 ",
                " synthetic-evidence-923 ", LocalDate.now().plusDays(30), " submitter ");

        BusinessPartnerScreening first = service.submitScreening(padded);
        BusinessPartnerScreening retried = service.submitScreening(padded);

        assertThat(first.jurisdiction()).isEqualTo("KR");
        assertThat(first.identifierType()).isEqualTo("BUSINESS_REGISTRATION");
        assertThat(first.identifier()).isEqualTo("1234567890");
        assertThat(first.evidenceReference()).isEqualTo("synthetic-evidence-923");
        assertThat(first.submittedBy()).isEqualTo("submitter");
        assertThat(retried).isSameAs(pending);
        verify(screenings, times(1)).save(any());
        verify(screenings, times(1)).appendAudit(any());
    }

    @Test
    void expiredPendingExactRetryIsNoOpButChangedExpiredEvidenceIsRejected() {
        when(partners.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        LocalDate expiredOn = LocalDate.now().minusDays(1);
        BusinessPartnerScreening pending = BusinessPartnerScreening.submit("BP-923", "KR",
                "BUSINESS_REGISTRATION", "1234567890", "synthetic-evidence-923",
                expiredOn, "submitter", expiredOn.minusDays(1).atTime(9, 0));
        when(screenings.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(pending));
        BusinessPartnerScreeningSubmission retry = new BusinessPartnerScreeningSubmission(
                " BP-923 ", "KR", " BUSINESS_REGISTRATION ", " 1234567890 ",
                " synthetic-evidence-923 ", expiredOn, " submitter ");
        BusinessPartnerScreeningSubmission changed = new BusinessPartnerScreeningSubmission(
                "BP-923", "KR", "BUSINESS_REGISTRATION", "1234567890",
                "synthetic-changed-evidence", expiredOn, "submitter");

        assertThat(service.submitScreening(retry)).isSameAs(pending);
        assertThatIllegalArgumentException().isThrownBy(() -> service.submitScreening(changed))
                .withMessageContaining("Evidence must not already be expired");
        verify(screenings, never()).save(any());
        verify(screenings, never()).appendAudit(any());
    }

    @Test
    void changedDecisionNeedsNewSubmissionAndSelfReviewIsRejectedWithoutWrite() {
        when(partners.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        when(screenings.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(pending()), Optional.of(pending().decide(
                        BusinessPartner.KycStatus.APPROVED, BusinessPartner.RiskRating.LOW,
                        "Checked", "reviewer", LocalDateTime.now())));

        assertThatIllegalArgumentException().isThrownBy(() -> service.decideScreening(
                new BusinessPartnerScreeningDecision("BP-923", BusinessPartner.KycStatus.APPROVED,
                        BusinessPartner.RiskRating.LOW, "Self review", "submitter")));
        assertThatIllegalStateException().isThrownBy(() -> service.decideScreening(
                new BusinessPartnerScreeningDecision("BP-923", BusinessPartner.KycStatus.REJECTED,
                        BusinessPartner.RiskRating.HIGH, "Changed", "reviewer")));
        verify(screenings, never()).save(any());
        verify(screenings, never()).appendAudit(any());
    }

    @Test
    void eligibilityRequiresActiveSupportedPartnerAndScreening() {
        LocalDateTime decisionTime = LocalDateTime.now();
        LocalDate date = decisionTime.toLocalDate();
        BusinessPartnerScreening approved = pending().decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW, "Checked", "reviewer", decisionTime);
        when(partners.findEffectiveByBusinessPartnerCode("BP-923", date))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, false)))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.BANK, true)));
        when(screenings.findLatestAt("BP-923", date)).thenReturn(Optional.of(approved));

        assertThat(service.isEligible("BP-923", date)).isTrue();
        assertThat(service.isEligible("BP-923", date)).isFalse();
        assertThat(service.isEligible("BP-923", date)).isFalse();
        assertThat(service.isEligible("MISSING", date)).isFalse();
        assertThat(service.isEligible("BP-923", null)).isFalse();
        verify(screenings, times(1)).findLatestAt("BP-923", date);
    }

    @Test
    void registrationChangeMakesStillActivePartnerIneligibleUntilNewScreening() {
        LocalDateTime decisionTime = LocalDateTime.now();
        LocalDate date = decisionTime.toLocalDate();
        BusinessPartnerScreening approved = pending().decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW, "Checked", "reviewer", decisionTime);
        BusinessPartner changed = partner(BusinessPartner.PartnerType.VENDOR, true, "987-65-43210");
        when(partners.findEffectiveByBusinessPartnerCode("BP-923", date))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)))
                .thenReturn(Optional.of(changed));
        when(screenings.findLatestAt("BP-923", date)).thenReturn(Optional.of(approved));

        assertThat(service.isEligible("BP-923", date)).isTrue();
        assertThat(changed.isActiveAt(date)).isTrue();
        assertThat(service.isEligible("BP-923", date)).isFalse();
    }

    @Test
    void historicalEligibilityReplaysApprovalBeforeLaterResubmissionAndRejection() {
        LocalDate approvedDate = LocalDate.now().minusDays(5);
        LocalDate resubmittedDate = LocalDate.now().minusDays(2);
        LocalDate rejectedDate = LocalDate.now();
        BusinessPartnerScreening firstPending = BusinessPartnerScreening.submit("BP-923", "KR",
                "BUSINESS_REGISTRATION", "1234567890", "synthetic-first-evidence",
                LocalDate.now().plusDays(30), "submitter", approvedDate.minusDays(1).atStartOfDay());
        BusinessPartnerScreening approved = firstPending.decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW, "First evidence checked", "reviewer",
                approvedDate.atTime(10, 0));
        BusinessPartnerScreening resubmitted = BusinessPartnerScreening.submit("BP-923", "KR",
                "BUSINESS_REGISTRATION", "1234567890", "synthetic-second-evidence",
                LocalDate.now().plusDays(30), "submitter", resubmittedDate.atTime(9, 0));
        BusinessPartnerScreening rejected = resubmitted.decide(BusinessPartner.KycStatus.REJECTED,
                BusinessPartner.RiskRating.HIGH, "Evidence failed", "reviewer",
                rejectedDate.atTime(9, 0));
        when(partners.findEffectiveByBusinessPartnerCode("BP-923", approvedDate))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        when(partners.findEffectiveByBusinessPartnerCode("BP-923", resubmittedDate))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        when(partners.findEffectiveByBusinessPartnerCode("BP-923", rejectedDate))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        when(screenings.findLatestAt("BP-923", approvedDate)).thenReturn(Optional.of(approved));
        when(screenings.findLatestAt("BP-923", resubmittedDate)).thenReturn(Optional.of(resubmitted));
        when(screenings.findLatestAt("BP-923", rejectedDate)).thenReturn(Optional.of(rejected));

        assertThat(service.isEligible("BP-923", approvedDate)).isTrue();
        assertThat(service.isEligible("BP-923", resubmittedDate)).isFalse();
        assertThat(service.isEligible("BP-923", rejectedDate)).isFalse();
    }

    @Test
    void futureApprovalCannotConferPastEligibility() {
        LocalDate beforeDecision = LocalDate.now().minusDays(1);
        BusinessPartnerScreening pending = BusinessPartnerScreening.submit("BP-923", "KR",
                "BUSINESS_REGISTRATION", "1234567890", "synthetic-evidence-923",
                LocalDate.now().plusDays(30), "submitter", beforeDecision.minusDays(1).atTime(9, 0));
        BusinessPartnerScreening futureApproval = pending.decide(BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.LOW, "Checked later", "reviewer", LocalDate.now().atTime(9, 0));
        when(partners.findEffectiveByBusinessPartnerCode("BP-923", beforeDecision))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        when(screenings.findLatestAt("BP-923", beforeDecision)).thenReturn(Optional.of(pending));

        assertThat(futureApproval.isEligible(beforeDecision)).isFalse();
        assertThat(service.isEligible("BP-923", beforeDecision)).isFalse();
        verify(screenings, never()).findByBusinessPartnerCode("BP-923");
    }

    @Test
    void unsupportedExtremeDatesFailClosedBeforeAnyPersistenceAccess() {
        assertThat(service.isEligible("BP-923", LocalDate.MIN)).isFalse();
        assertThat(service.isEligible("BP-923", LocalDate.MAX)).isFalse();
        verifyNoInteractions(partners, screenings, businessKeyLock);
    }

    @Test
    void invalidResubmissionFailsWithoutLeakingIdentifierOrAppendingAudit() {
        when(partners.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(partner(BusinessPartner.PartnerType.VENDOR, true)));
        when(screenings.findByBusinessPartnerCodeForUpdate("BP-923"))
                .thenReturn(Optional.of(pending()));

        assertThatIllegalArgumentException().isThrownBy(() -> service.submitScreening(
                new BusinessPartnerScreeningSubmission("BP-923", null, "BUSINESS_REGISTRATION",
                        "1234567890", "synthetic-evidence-923", LocalDate.now().plusDays(30), "submitter")))
                .withMessageNotContaining("1234567890");
        verify(screenings, never()).save(any());
        verify(screenings, never()).appendAudit(any());
    }

    private BusinessPartnerScreeningSubmission submission() {
        return new BusinessPartnerScreeningSubmission("BP-923", "KR", "BUSINESS_REGISTRATION",
                "1234567890", "synthetic-evidence-923", LocalDate.now().plusDays(30), "submitter");
    }

    private BusinessPartnerScreeningDecision decision() {
        return new BusinessPartnerScreeningDecision("BP-923", BusinessPartner.KycStatus.APPROVED,
                BusinessPartner.RiskRating.MEDIUM, "Evidence checked", "reviewer");
    }

    private BusinessPartnerScreening pending() {
        return BusinessPartnerScreening.submit("BP-923", "KR", "BUSINESS_REGISTRATION",
                "1234567890", "synthetic-evidence-923", LocalDate.now().plusDays(30),
                "submitter", LocalDateTime.now().minusMinutes(5));
    }

    private BusinessPartner partner(BusinessPartner.PartnerType type, boolean active) {
        return partner(type, active, "123-45-67890");
    }

    private BusinessPartner partner(BusinessPartner.PartnerType type, boolean active, String registration) {
        return BusinessPartner.reconstitute(923L, "BP-923", "Synthetic partner", registration,
                "Synthetic CEO", "Trade", "Materials", type, active,
                BusinessPartner.KycStatus.APPROVED, BusinessPartner.RiskRating.LOW,
                LocalDate.now().minusYears(1), BusinessPartner.OPEN_ENDED_VALID_TO,
                LocalDateTime.now().minusYears(1), LocalDateTime.now().minusYears(1),
                "fixture", List.of());
    }
}
