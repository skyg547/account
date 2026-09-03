package com.ho.account.internalaudit.core.application;

/**
 * Request-scoped context holder for authenticated actor and tracing lineage
 * (actor ID from #449 X-Auth-User, correlation ID, idempotency key).
 */
public final class AuditActorContext {

    private static final ThreadLocal<String> CURRENT_ACTOR = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_IDEMPOTENCY_KEY = new ThreadLocal<>();

    private AuditActorContext() {}

    public static void setActor(String actor) {
        if (actor != null && !actor.isBlank()) {
            CURRENT_ACTOR.set(actor.trim());
        } else {
            CURRENT_ACTOR.remove();
        }
    }

    public static String getActor() {
        return CURRENT_ACTOR.get();
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            CURRENT_CORRELATION_ID.set(correlationId.trim());
        } else {
            CURRENT_CORRELATION_ID.remove();
        }
    }

    public static String getCorrelationId() {
        return CURRENT_CORRELATION_ID.get();
    }

    public static void setIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            CURRENT_IDEMPOTENCY_KEY.set(idempotencyKey.trim());
        } else {
            CURRENT_IDEMPOTENCY_KEY.remove();
        }
    }

    public static String getIdempotencyKey() {
        return CURRENT_IDEMPOTENCY_KEY.get();
    }

    public static void clear() {
        CURRENT_ACTOR.remove();
        CURRENT_CORRELATION_ID.remove();
        CURRENT_IDEMPOTENCY_KEY.remove();
    }
}
