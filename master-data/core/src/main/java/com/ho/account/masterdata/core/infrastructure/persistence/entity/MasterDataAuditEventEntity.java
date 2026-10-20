package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaMasterDataAuditAdapter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/** Insert-only mapping. No setters are exposed for committed evidence. */
@Entity
@Table(name = "master_data_audit_events")
@Getter
public class MasterDataAuditEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "master_data_audit_event_ids")
    @SequenceGenerator(name = "master_data_audit_event_ids", sequenceName = "master_data_audit_event_ids", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 40)
    private String eventType;
    @Column(nullable = false)
    private Instant occurredAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private MasterDataType targetType;
    @Column(nullable = false, length = 100)
    private String targetKey;
    private Long requestId;
    @Column(length = 120)
    private String sourceReference;
    @Column(length = 80)
    private String serviceActor;
    @Column(nullable = false, length = 80)
    private String businessActor;
    @Column(length = 500)
    private String reason;
    private Long beforeVersionId;
    private Long afterVersionId;
    @Column(nullable = false, length = 64)
    private String integrityHash;

    protected MasterDataAuditEventEntity() { }

    public MasterDataAuditEventEntity(String eventType, Instant occurredAt, MasterDataType targetType,
            String targetKey, Long requestId, String sourceReference, String serviceActor,
            String businessActor, String reason, Long beforeVersionId, Long afterVersionId,
            String integrityHash) {
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.targetType = targetType;
        this.targetKey = targetKey;
        this.requestId = requestId;
        this.sourceReference = sourceReference;
        this.serviceActor = serviceActor;
        this.businessActor = businessActor;
        this.reason = reason;
        this.beforeVersionId = beforeVersionId;
        this.afterVersionId = afterVersionId;
        this.integrityHash = integrityHash;
    }

    /** A sequence ID is available before INSERT, so the writer needs no UPDATE privilege. */
    @PrePersist
    void sealBeforeInsert() {
        if (integrityHash != null || id == null) {
            throw new IllegalStateException("Audit event must be sealed once with an allocated ID");
        }
        integrityHash = JpaMasterDataAuditAdapter.fingerprint(id,
                new MasterDataAuditEvent(eventType, occurredAt, targetType, targetKey, requestId,
                        sourceReference, serviceActor, businessActor, reason, beforeVersionId, afterVersionId));
    }
}
