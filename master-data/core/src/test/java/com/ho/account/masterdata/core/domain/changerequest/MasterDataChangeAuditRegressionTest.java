package com.ho.account.masterdata.core.domain.changerequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MasterDataChangeAuditRegressionTest {

    @Test
    void rejectionPreservesOriginalReasonAndRecordsDecisionReason() {
        MasterDataChangeRequest request = new MasterDataChangeRequest(
                MasterDataChangeRequest.MasterDataType.PRODUCT, "P-924",
                MasterDataChangeRequest.ChangeType.DEACTIVATE, LocalDate.now(), 1,
                "requester-924", "Original business reason", null, "source-924");

        request.reject("reviewer-924", "Decision reason");

        assertThat(request.getReason()).isEqualTo("Original business reason");
        assertThat(request.getDecisionReason()).isEqualTo("Decision reason");
        assertThat(request.getApprovedBy()).isEqualTo("reviewer-924");
        assertThat(request.getApprovedAt()).isNotNull();
    }
}
