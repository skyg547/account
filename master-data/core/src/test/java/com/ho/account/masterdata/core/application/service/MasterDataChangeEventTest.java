package com.ho.account.masterdata.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MasterDataChangeEventTest {
    @Test
    void workflowEventRejectsRawSecretBearingReason() {
        assertThatThrownBy(() -> new MasterDataAuditEvent("REJECTED", Instant.now(),
                MasterDataType.PRODUCT, "P-924", 9L, "source-924", null,
                "reviewer-924", "synthetic credential=secret-924", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digest reference");
    }

    @Test
    void missingPersistedVersionPreventsAppliedStatusAndSuccessEvent() {
        var requests = mock(MasterDataChangeRequestPersistencePort.class);
        var versions = mock(MasterDataVersionQueryPort.class);
        var audit = mock(MasterDataAuditPort.class);
        MasterDataChangeRequest request = MasterDataChangeRequest.reconstitute(9L, MasterDataType.PRODUCT,
                "P-924", ChangeType.DEACTIVATE, MasterDataChangeRequest.ChangeStatus.APPROVED,
                0, LocalDate.now(), 1, "requester-924", "reviewer-924", null, null,
                "rationale", null, "source-924", null);
        when(requests.findById(9L)).thenReturn(Optional.of(request));
        when(requests.findByIdForUpdate(9L)).thenReturn(Optional.of(request));
        when(versions.countPersistedVersions(MasterDataType.PRODUCT, "P-924")).thenReturn(1L);
        MasterDataChangeApplier applier = mock(MasterDataChangeApplier.class);
        when(applier.targetType()).thenReturn(MasterDataType.PRODUCT);
        var service = new MasterDataChangeRequestService(requests, versions, List.of(applier),
                mock(MasterDataBusinessKeyLockPort.class), audit);

        assertThatThrownBy(() -> service.applyApprovedChange(9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version identifiers");
        assertThat(request.getStatus()).isEqualTo(MasterDataChangeRequest.ChangeStatus.APPROVED);
        verify(requests, never()).save(any());
        verify(audit, never()).append(any());
    }

    @Test
    void approvalAndApplyRetainRequestLineageAndVersionIdsWithoutPayload() {
        var requests = mock(MasterDataChangeRequestPersistencePort.class);
        var versions = mock(MasterDataVersionQueryPort.class);
        var audit = mock(MasterDataAuditPort.class);
        var request = new MasterDataChangeRequest(MasterDataType.PRODUCT, "P-924",
                ChangeType.DEACTIVATE, LocalDate.now(), 1, "requester-924",
                "business rationale with synthetic credential=secret-924", null, "source-924");
        request = MasterDataChangeRequest.reconstitute(9L, request.getTargetType(), request.getTargetKey(),
                request.getChangeType(), request.getStatus(), 0, request.getEffectiveDate(),
                request.getRequestedVersion(), request.getRequestedBy(), null, request.getRequestedAt(),
                null, request.getReason(), null, request.getSourceReference(), null);
        when(requests.findById(9L)).thenReturn(Optional.of(request));
        when(requests.findByIdForUpdate(9L)).thenReturn(Optional.of(request));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(versions.countPersistedVersions(MasterDataType.PRODUCT, "P-924")).thenReturn(1L);
        MasterDataChangeApplier applier = new MasterDataChangeApplier() {
            public MasterDataType targetType() { return MasterDataType.PRODUCT; }
            public void validate(MasterDataChangeRequest ignored) { }
            public AppliedVersion apply(MasterDataChangeRequest ignored) { return new AppliedVersion(41L, 41L); }
        };
        var service = new MasterDataChangeRequestService(requests, versions, List.of(applier),
                mock(MasterDataBusinessKeyLockPort.class), audit);

        service.approve(9L, "reviewer-924");
        service.applyApprovedChange(9L);

        ArgumentCaptor<MasterDataAuditEvent> captured = ArgumentCaptor.forClass(MasterDataAuditEvent.class);
        verify(audit, org.mockito.Mockito.times(2)).append(captured.capture());
        var approved = captured.getAllValues().get(0);
        var applied = captured.getAllValues().get(1);
        assertThat(approved.eventType()).isEqualTo("APPROVED");
        assertThat(approved.businessActor()).isEqualTo("reviewer-924");
        assertThat(approved.reason()).startsWith("request:sha256:");
        assertThat(approved.reason()).doesNotContain("secret-924");
        assertThat(applied.eventType()).isEqualTo("APPLIED");
        assertThat(applied.requestId()).isEqualTo(9L);
        assertThat(applied.sourceReference()).isEqualTo("source-924");
        assertThat(applied.beforeVersionId()).isEqualTo(41L);
        assertThat(applied.afterVersionId()).isEqualTo(41L);
        assertThat(applied.serviceActor()).isEqualTo("master-data-scheduler");
        assertThat(applied.reason()).isEqualTo(approved.reason());
        assertThat(request.getReason()).contains("secret-924");
    }
}
