package com.ho.account.masterdata.core.domain.changerequest;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterDataChangeRequestTest {

    @Test
    void createsRequestedChangeRequest() {
        MasterDataChangeRequest request = sampleRequest();

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.REQUESTED);
        assertThat(request.getRequestedAt()).isNotNull();
        assertThat(request.getRequestedVersion()).isEqualTo(1);
    }

    @Test
    void approvesWhenApproverIsDifferentFromRequester() {
        MasterDataChangeRequest request = sampleRequest();

        request.approve("manager");

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.APPROVED);
        assertThat(request.getApprovedBy()).isEqualTo("manager");
        assertThat(request.getApprovedAt()).isNotNull();
    }

    @Test
    void rejectsSameRequesterApprovalForSodControl() {
        MasterDataChangeRequest request = sampleRequest();

        assertThatThrownBy(() -> request.approve("operator"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void appliesOnlyAfterApproval() {
        MasterDataChangeRequest request = sampleRequest();

        assertThatThrownBy(request::markApplied)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");

        request.approve("manager");
        request.markApplied();

        assertThat(request.getStatus()).isEqualTo(ChangeStatus.APPLIED);
        assertThat(request.getAppliedAt()).isNotNull();
    }

    @Test
    void createAndUpdateRequirePayloadButDeactivateDoesNot() {
        assertThatThrownBy(() -> new MasterDataChangeRequest(
                MasterDataType.ACCOUNT_SUBJECT,
                "101000",
                ChangeType.UPDATE,
                LocalDate.now(),
                2,
                "operator",
                "Update account subject",
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payload is required");

        MasterDataChangeRequest deactivate = new MasterDataChangeRequest(
                MasterDataType.ACCOUNT_SUBJECT,
                "101000",
                ChangeType.DEACTIVATE,
                LocalDate.now(),
                1,
                "operator",
                "Deactivate account subject",
                "unused payload");
        assertThat(deactivate.getPayloadJson()).isNull();
    }

    @Test
    void enforcesPersistenceLengthsForNonHttpCallers() {
        assertThatThrownBy(() -> new MasterDataChangeRequest(
                MasterDataType.ACCOUNT_SUBJECT,
                "K".repeat(101),
                ChangeType.CREATE,
                LocalDate.now(),
                1,
                "operator",
                null,
                "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target key");
    }

    @Test
    void requesterCannotRejectOwnRequestAndRejectReasonIsRequired() {
        MasterDataChangeRequest ownDecision = sampleRequest();
        assertThatThrownBy(() -> ownDecision.reject("operator", "No longer needed"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be different");

        MasterDataChangeRequest missingReason = sampleRequest();
        assertThatThrownBy(() -> missingReason.reject("manager", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reject reason is required");
    }

    private MasterDataChangeRequest sampleRequest() {
        return new MasterDataChangeRequest(
                MasterDataType.ACCOUNT_SUBJECT,
                "101000",
                ChangeType.UPDATE,
                LocalDate.now().plusDays(1),
                1,
                "operator",
                "Update account subject",
                "{\"name\":\"Cash and cash equivalents\"}");
    }
}
