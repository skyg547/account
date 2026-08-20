package com.ho.account.shared.infrastructure.security.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 마스터 승인 이력의 상태 전이와 SOD(자기승인 금지) 규칙을 검증합니다.
 */
class MasterApprovalTest {

    private static final LocalDateTime DECIDED_AT = LocalDateTime.of(2026, 8, 13, 10, 30);

    private static MasterApproval pendingApproval(String requestUser) {
        MasterApproval approval = new MasterApproval();
        approval.setMasterType("ACCOUNT_SUBJECT");
        approval.setMasterKey("101010");
        approval.setRequestType(MasterApproval.ChangeRequestType.UPDATE);
        approval.setRequestUser(requestUser);
        approval.setStatus(MasterApproval.ApprovalStatus.PENDING);
        return approval;
    }

    @Test
    void approveTransitionsPendingToApprovedAndRecordsApprover() {
        MasterApproval approval = pendingApproval("requester");

        approval.approve("approver", "확인 완료", DECIDED_AT);

        assertThat(approval.getStatus()).isEqualTo(MasterApproval.ApprovalStatus.APPROVED);
        assertThat(approval.getApproverUser()).isEqualTo("approver");
        assertThat(approval.getApprovalDate()).isEqualTo(DECIDED_AT);
        assertThat(approval.getRemarks()).isEqualTo("확인 완료");
        assertThat(approval.getAuditUser()).isEqualTo("approver");
    }

    @Test
    void approveBlocksSelfApprovalAsSodViolation() {
        MasterApproval approval = pendingApproval("same-user");

        assertThatThrownBy(() -> approval.approve("same-user", null, DECIDED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Self-approval is not allowed");

        assertThat(approval.getStatus()).isEqualTo(MasterApproval.ApprovalStatus.PENDING);
    }

    @Test
    void approveBlocksSelfApprovalEvenWhenApproverIsPaddedWithWhitespace() {
        MasterApproval approval = pendingApproval("same-user");

        assertThatThrownBy(() -> approval.approve("  same-user  ", null, DECIDED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Self-approval is not allowed");
    }

    @Test
    void approveRejectsNonPendingStatus() {
        MasterApproval approval = pendingApproval("requester");
        approval.approve("approver", null, DECIDED_AT);

        assertThatThrownBy(() -> approval.approve("another-approver", null, DECIDED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING approval can be approved.");

        assertThat(approval.getApproverUser()).isEqualTo("approver");
    }

    @Test
    void approveRejectsBlankApprover() {
        MasterApproval approval = pendingApproval("requester");

        assertThatThrownBy(() -> approval.approve("   ", null, DECIDED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Approver user is required.");

        assertThat(approval.getStatus()).isEqualTo(MasterApproval.ApprovalStatus.PENDING);
    }

    @Test
    void approveFallsBackToCurrentTimeWhenApprovedAtIsNull() {
        MasterApproval approval = pendingApproval("requester");
        LocalDateTime before = LocalDateTime.now();

        approval.approve("approver", null, null);

        assertThat(approval.getApprovalDate()).isNotNull();
        assertThat(approval.getApprovalDate()).isAfterOrEqualTo(before);
    }

    @Test
    void rejectTransitionsPendingToRejected() {
        MasterApproval approval = pendingApproval("requester");

        approval.reject("approver", "근거 부족", DECIDED_AT);

        assertThat(approval.getStatus()).isEqualTo(MasterApproval.ApprovalStatus.REJECTED);
        assertThat(approval.getApproverUser()).isEqualTo("approver");
        assertThat(approval.getApprovalDate()).isEqualTo(DECIDED_AT);
        assertThat(approval.getRemarks()).isEqualTo("근거 부족");
        assertThat(approval.getAuditUser()).isEqualTo("approver");
    }

    @Test
    void rejectRejectsNonPendingStatus() {
        MasterApproval approval = pendingApproval("requester");
        approval.reject("approver", null, DECIDED_AT);

        assertThatThrownBy(() -> approval.reject("approver", null, DECIDED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING approval can be rejected.");
    }

    @Test
    void rejectRejectsBlankApprover() {
        MasterApproval approval = pendingApproval("requester");

        assertThatThrownBy(() -> approval.reject(null, null, DECIDED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Approver user is required.");
    }

    /**
     * 현재 구현은 반려 경로에 자기승인(SOD) 검사를 두지 않는다. 승인 경로와 비대칭이므로
     * 의도된 동작인지 확인이 필요하며, 이 테스트는 현재 계약을 고정해 변경을 감지한다.
     */
    @Test
    void rejectCurrentlyAllowsSelfRejectionUnlikeApprove() {
        MasterApproval approval = pendingApproval("same-user");

        approval.reject("same-user", null, DECIDED_AT);

        assertThat(approval.getStatus()).isEqualTo(MasterApproval.ApprovalStatus.REJECTED);
    }
}
