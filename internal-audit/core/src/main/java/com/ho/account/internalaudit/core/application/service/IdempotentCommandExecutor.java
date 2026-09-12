package com.ho.account.internalaudit.core.application.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.CommandReceipt;
import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates a command, its first result and audit lineage in the caller's transaction. */
@Service
public class IdempotentCommandExecutor {

    private static final int FINGERPRINT_VERSION = 1;
    private static final int SNAPSHOT_VERSION = 1;
    private static final String INTEGRITY_ERROR = "Unable to process the command.";

    private final CommandReceiptPort receipts;
    private final AuditLogPersistencePort audits;
    private final ObjectMapper mapper;

    @Autowired
    public IdempotentCommandExecutor(CommandReceiptPort receipts, AuditLogPersistencePort audits,
                                     ObjectMapper mapper) {
        this.receipts = Objects.requireNonNull(receipts);
        this.audits = Objects.requireNonNull(audits);
        this.mapper = snapshotMapper(mapper);
    }

    private IdempotentCommandExecutor(AuditLogPersistencePort audits, ObjectMapper mapper) {
        this.receipts = null;
        this.audits = audits;
        this.mapper = snapshotMapper(mapper);
    }

    private static ObjectMapper snapshotMapper(ObjectMapper mapper) {
        // Isolate the persisted v1 format from HTTP null omission and ordering settings.
        return Objects.requireNonNull(mapper).copy()
                .setSerializationInclusion(JsonInclude.Include.ALWAYS)
                .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .configure(SerializationFeature.INDENT_OUTPUT, false)
                .configure(SerializationFeature.WRAP_ROOT_VALUE, false)
                .configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false)
                .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
                .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true)
                .configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
    }

    /** Compatibility for direct, unkeyed unit callers; production wiring requires all ports. */
    static IdempotentCommandExecutor unkeyedOnly(AuditLogPersistencePort audits, ObjectMapper mapper) {
        return new IdempotentCommandExecutor(audits, mapper);
    }

    @Transactional
    public <T> T execute(String actor, String action, String aggregateType, String aggregateId,
                         String effectivePath, Object effectiveCommand, Class<T> resultType,
                         Supplier<T> command) {
        String key = AuditActorContext.getIdempotencyKey();
        if (key == null || key.isBlank()) {
            T result = command.get();
            if (audits != null) {
                appendAudit(actor, action, aggregateType, aggregateId, null, encodeSnapshot(result, resultType));
            }
            return result;
        }
        key = key.trim();
        if (key.length() > 255) {
            throw new IllegalArgumentException("Idempotency key must contain 1 to 255 characters");
        }
        if (receipts == null) {
            // Legacy constructors must never turn a keyed invocation into an unprotected write.
            throw integrityFailure(null);
        }
        String fingerprint = fingerprint(actor, action, aggregateType, aggregateId, effectivePath, effectiveCommand);
        // Lock absent keys too, before either lookup. Direct audit appends share this lock.
        receipts.lockKey(key);
        Optional<CommandReceipt> existing = receipts.findByKey(key);
        if (existing.isPresent()) {
            CommandReceipt receipt = existing.get();
            requireCompleteReceipt(key, receipt);
            if (!fingerprint.equals(receipt.fingerprint())) {
                throw new IdempotencyConflictException();
            }
            requireReplayAudit(key, actor, action, aggregateType, aggregateId, receipt.snapshotJson());
            return decodeSnapshot(receipt.snapshotJson(), resultType);
        }
        if (audits.findByIdempotencyKey(key).isPresent()) {
            // A historical audit result cannot prove which effective request first used the key.
            throw new IdempotencyConflictException();
        }
        receipts.reserve(key, FINGERPRINT_VERSION, fingerprint);
        T result = command.get();
        String snapshot = encodeSnapshot(result, resultType);
        appendAudit(actor, action, aggregateType, aggregateId, key, snapshot);
        receipts.complete(key, SNAPSHOT_VERSION, snapshot);
        return result;
    }

    private String fingerprint(String actor, String action, String aggregateType, String aggregateId,
                               String effectivePath, Object command) {
        try {
            ObjectNode identity = mapper.createObjectNode();
            identity.put("version", FINGERPRINT_VERSION);
            identity.put("actor", actor);
            identity.put("action", action);
            identity.put("aggregateType", aggregateType);
            identity.put("aggregateId", aggregateId);
            identity.put("effectivePath", effectivePath);
            identity.set("payload", canonicalize(mapper.valueToTree(command)));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsString(identity).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw integrityFailure(failure);
        }
    }

    private JsonNode canonicalize(JsonNode value) {
        if (value.isObject()) {
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            value.fields().forEachRemaining(entry -> fields.put(entry.getKey(), canonicalize(entry.getValue())));
            ObjectNode sorted = mapper.createObjectNode();
            fields.forEach(sorted::set);
            return sorted;
        }
        if (value.isArray()) {
            ArrayNode array = mapper.createArrayNode();
            value.forEach(item -> array.add(canonicalize(item)));
            return array;
        }
        return value;
    }

    private void requireReplayAudit(String key, String actor, String action, String aggregateType,
                                    String aggregateId, String snapshot) {
        AuditLogEntry audit = audits.findByIdempotencyKey(key).orElseThrow(() -> integrityFailure(null));
        // The immutable first audit independently anchors the stored result; retry trace/time may differ.
        if (!Objects.equals(actor, audit.actor()) || !Objects.equals(action, audit.action())
                || !Objects.equals(aggregateType, audit.aggregateType())
                || !Objects.equals(aggregateId, audit.aggregateId())
                || !Objects.equals(key, audit.idempotencyKey()) || !Objects.equals(snapshot, audit.detailsJson())) {
            throw integrityFailure(null);
        }
    }

    private void requireCompleteReceipt(String key, CommandReceipt receipt) {
        if (!key.equals(receipt.idempotencyKey()) || receipt.fingerprintVersion() != FINGERPRINT_VERSION
                || receipt.fingerprint() == null || !receipt.fingerprint().matches("[0-9a-f]{64}")
                || receipt.snapshotVersion() != SNAPSHOT_VERSION
                || receipt.snapshotJson() == null || receipt.snapshotJson().isBlank()) {
            throw integrityFailure(null);
        }
    }

    private <T> String encodeSnapshot(T result, Class<T> resultType) {
        try {
            String json = mapper.writeValueAsString(Objects.requireNonNull(result));
            // Verify full restoration before appending an audit or marking the receipt complete.
            decodeSnapshot(json, resultType);
            return json;
        } catch (Exception failure) {
            throw integrityFailure(failure);
        }
    }

    private <T> T decodeSnapshot(String json, Class<T> resultType) {
        try {
            JsonNode tree = mapper.readTree(json);
            if (tree == null || !tree.isObject() || !resultType.isRecord()) {
                throw integrityFailure(null);
            }
            requireSnapshotShape(tree, mapper.constructType(resultType));
            T result = mapper.treeToValue(tree, resultType);
            // Constructor normalization or coercion must not silently repair stored corruption.
            if (!tree.equals(mapper.valueToTree(result))) {
                throw integrityFailure(null);
            }
            return result;
        } catch (Exception failure) {
            throw integrityFailure(failure);
        }
    }

    private void requireSnapshotShape(JsonNode value, JavaType type) {
        if (value.isNull()) {
            if (type.isPrimitive()) {
                throw integrityFailure(null);
            }
            return;
        }
        Class<?> raw = type.getRawClass();
        if (raw.isRecord()) {
            RecordComponent[] fields = raw.getRecordComponents();
            if (!value.isObject() || value.size() != fields.length) {
                throw integrityFailure(null);
            }
            for (RecordComponent field : fields) {
                // Missing nullable fields are corruption; explicit JSON null is a real saved value.
                if (!value.has(field.getName())) {
                    throw integrityFailure(null);
                }
                requireSnapshotShape(value.get(field.getName()), mapper.constructType(field.getGenericType()));
            }
        } else if (raw == String.class) {
            if (!value.isTextual()) {
                throw integrityFailure(null);
            }
        } else if (raw == Integer.class || raw == int.class) {
            if (!value.isIntegralNumber() || !value.canConvertToInt()) {
                throw integrityFailure(null);
            }
        } else if (type.isCollectionLikeType()) {
            if (!value.isArray()) {
                throw integrityFailure(null);
            }
            value.forEach(item -> requireSnapshotShape(item, type.getContentType()));
        } else {
            throw integrityFailure(null);
        }
    }

    private void appendAudit(String actor, String action, String aggregateType, String aggregateId,
                             String key, String snapshot) {
        AuditLogEntry entry = AuditLogEntry.builder()
                .actor(actor).action(action).aggregateType(aggregateType).aggregateId(aggregateId)
                .actionTimestamp(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .correlationId(AuditActorContext.getCorrelationId()).idempotencyKey(key)
                .detailsJson(snapshot).build();
        AuditLogEntry persisted = audits.append(entry);
        // A port returning another event must not let business changes commit as if audited.
        if (persisted == null || !Objects.equals(persisted.toBuilder().id(null).build(), entry)) {
            throw integrityFailure(null);
        }
    }

    private IllegalStateException integrityFailure(Throwable cause) {
        return new IllegalStateException(INTEGRITY_ERROR, cause);
    }
}
