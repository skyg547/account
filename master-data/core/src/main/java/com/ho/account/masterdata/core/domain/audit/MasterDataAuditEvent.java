package com.ho.account.masterdata.core.domain.audit;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.Instant;
import java.util.Objects;

/** Only identifiers and decision metadata are retained; raw change payloads never enter the event stream. */
public record MasterDataAuditEvent(
        String eventType, Instant occurredAt, MasterDataType targetType, String targetKey,
        Long requestId, String sourceReference, String serviceActor, String businessActor,
        String reason, Long beforeVersionId, Long afterVersionId) {
    public MasterDataAuditEvent {
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(targetType);
        Objects.requireNonNull(targetKey);
        if (businessActor == null || businessActor.isBlank()) {
            throw new IllegalArgumentException("Stable business actor is required");
        }
        if (java.util.Set.of("REQUESTED", "APPROVED", "REJECTED", "APPLIED").contains(eventType)
                && reason != null && !reason.matches("(request|decision):sha256:[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Workflow audit reason must be a digest reference");
        }
        if (eventType.length() > 40 || targetKey.length() > 100 || businessActor.length() > 80
                || (serviceActor != null && serviceActor.length() > 80)
                || (sourceReference != null && sourceReference.length() > 120)
                || (reason != null && reason.length() > 500)) {
            throw new IllegalArgumentException("Audit metadata exceeds column limit");
        }
    }
}
