package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Records direct writes with verified ingress identity and row identifiers. */
final class MasterDataAuditRecorder {
    private MasterDataAuditRecorder() { }

    static void direct(MasterDataAuditPort port, String operation, MasterDataType type, String key,
            Long beforeId, Long afterId) {
        MasterDataActorContext.Actor actor = MasterDataActorContext.current();
        // The outer change-request use case emits one APPLIED event with both row IDs and lineage.
        if (actor != null && actor.approvedChange()) return;
        port.append(new MasterDataAuditEvent(operation, Instant.now(), type, key, null, null,
                actor == null ? null : actor.serviceActor(),
                actor == null ? "SYSTEM" : actor.businessActor(), null, beforeId, afterId));
    }

    static String reasonReference(String eventType, String rawReason) {
        if (rawReason == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String kind = "REJECTED".equals(eventType) ? "decision" : "request";
            return kind + ":sha256:" + HexFormat.of().formatHex(
                    digest.digest(rawReason.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
