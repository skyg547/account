package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.MasterDataAuditEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaMasterDataAuditAdapter implements MasterDataAuditPort {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(MasterDataAuditEvent event) {
        // MANDATORY makes an audit insert fail with the business write if no shared transaction exists.
        // PostgreSQL/H2 timestamps store microseconds; hash exactly the representation they persist.
        MasterDataAuditEvent stored = new MasterDataAuditEvent(event.eventType(),
                event.occurredAt().truncatedTo(ChronoUnit.MICROS), event.targetType(), event.targetKey(),
                event.requestId(), event.sourceReference(), event.serviceActor(), event.businessActor(),
                event.reason(), event.beforeVersionId(), event.afterVersionId());
        MasterDataAuditEventEntity row = new MasterDataAuditEventEntity(stored.eventType(), stored.occurredAt(),
                stored.targetType(), stored.targetKey(), stored.requestId(), stored.sourceReference(),
                stored.serviceActor(), stored.businessActor(), stored.reason(), stored.beforeVersionId(),
                stored.afterVersionId(), null);
        entityManager.persist(row);
    }

    public static String fingerprint(long id, MasterDataAuditEvent event) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = String.join("", component(id), component(event.eventType()), component(event.occurredAt()),
                    component(event.targetType().name()), component(event.targetKey()), component(event.requestId()),
                    component(event.sourceReference()), component(event.serviceActor()), component(event.businessActor()),
                    component(event.reason()), component(event.beforeVersionId()), component(event.afterVersionId()));
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** A bounded, ordered export detects accidental changes before evidence leaves the database. */
    @Transactional(readOnly = true)
    public List<ExportRow> exportAfterId(long afterId, int limit) {
        if (afterId < 0 || limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Audit export requires a positive page size up to 1000");
        }
        return entityManager.createQuery("select e from MasterDataAuditEventEntity e where e.id > :afterId order by e.id",
                        MasterDataAuditEventEntity.class)
                .setParameter("afterId", afterId).setMaxResults(limit).getResultList().stream()
                .map(this::verifiedRow).toList();
    }

    private ExportRow verifiedRow(MasterDataAuditEventEntity row) {
        MasterDataAuditEvent event = new MasterDataAuditEvent(row.getEventType(), row.getOccurredAt(),
                row.getTargetType(), row.getTargetKey(), row.getRequestId(), row.getSourceReference(),
                row.getServiceActor(), row.getBusinessActor(), row.getReason(),
                row.getBeforeVersionId(), row.getAfterVersionId());
        if (!fingerprint(row.getId(), event).equals(row.getIntegrityHash())) {
            throw new IllegalStateException("Master-data audit integrity mismatch at event ID " + row.getId());
        }
        return new ExportRow(row.getId(), event, row.getIntegrityHash());
    }

    public record ExportRow(Long id, MasterDataAuditEvent event, String integrityHash) { }

    private static String component(Object item) {
        if (item == null) return "-1:";
        String value = item.toString();
        return value.length() + ":" + value;
    }
}
