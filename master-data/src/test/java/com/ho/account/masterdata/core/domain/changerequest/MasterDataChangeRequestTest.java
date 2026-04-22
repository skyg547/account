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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("요청자와 승인자");
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
    }

    private MasterDataChangeRequest sampleRequest() {
        return new MasterDataChangeRequest(
                MasterDataType.ACCOUNT_SUBJECT,
                "101000",
                ChangeType.UPDATE,
                LocalDate.now().plusDays(1),
                1,
                "operator",
                "계정명 변경",
                "{\"name\":\"현금및현금성자산\"}");
    }
}
