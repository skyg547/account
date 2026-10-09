package com.ho.account.closing.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root for the lifecycle of a single business date.
 */

public class DailyClosingStatus {

    private LocalDate businessDate;

    private EodState state;

    private Long version;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;

    private LocalDateTime preparedAt;

    private String preparedBy;

    private LocalDateTime closingStartedAt;

    private String closingStartedBy;

    private LocalDateTime closedAt;

    private String closedBy;

    private LocalDateTime bodStartedAt;

    private String bodStartedBy;

    private LocalDateTime openedAt;

    private String openedBy;

    /** Rebuilds the state read by the persistence adapter without replaying transitions. */
    public static DailyClosingStatus restore(
            LocalDate businessDate,
            EodState state,
            Long version,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            LocalDateTime preparedAt,
            String preparedBy,
            LocalDateTime closingStartedAt,
            String closingStartedBy,
            LocalDateTime closedAt,
            String closedBy,
            LocalDateTime bodStartedAt,
            String bodStartedBy,
            LocalDateTime openedAt,
            String openedBy) {
        DailyClosingStatus status = new DailyClosingStatus();
        status.businessDate = businessDate;
        status.state = state;
        status.version = version;
        status.createdAt = createdAt;
        status.createdBy = createdBy;
        status.updatedAt = updatedAt;
        status.updatedBy = updatedBy;
        status.preparedAt = preparedAt;
        status.preparedBy = preparedBy;
        status.closingStartedAt = closingStartedAt;
        status.closingStartedBy = closingStartedBy;
        status.closedAt = closedAt;
        status.closedBy = closedBy;
        status.bodStartedAt = bodStartedAt;
        status.bodStartedBy = bodStartedBy;
        status.openedAt = openedAt;
        status.openedBy = openedBy;
        return status;
    }

    protected DailyClosingStatus() {
    }

    public static DailyClosingStatus bootstrap(
            LocalDate businessDate,
            String actor,
            LocalDateTime occurredAt) {
        String normalizedActor = requireActor(actor);
        DailyClosingStatus status = new DailyClosingStatus();
        status.initialize(businessDate, EodState.OPEN, normalizedActor, occurredAt);
        status.openedAt = occurredAt;
        status.openedBy = normalizedActor;
        return status;
    }

    public static DailyClosingStatus beginBusinessDay(
            LocalDate businessDate,
            String actor,
            LocalDateTime occurredAt) {
        String normalizedActor = requireActor(actor);
        DailyClosingStatus status = new DailyClosingStatus();
        status.initialize(businessDate, EodState.BOD_IN_PROGRESS, normalizedActor, occurredAt);
        status.bodStartedAt = occurredAt;
        status.bodStartedBy = normalizedActor;
        return status;
    }

    public void prepareEod(String actor, LocalDateTime occurredAt) {
        String normalizedActor = transitionTo(EodState.PRE_CLOSING, actor, occurredAt);
        preparedAt = occurredAt;
        preparedBy = normalizedActor;
    }

    public void cancelEodPreparation(String actor, LocalDateTime occurredAt) {
        String normalizedActor = transitionTo(EodState.OPEN, actor, occurredAt);
        openedAt = occurredAt;
        openedBy = normalizedActor;
    }

    public void startEod(String actor, LocalDateTime occurredAt) {
        String normalizedActor = transitionTo(EodState.CLOSING_IN_PROGRESS, actor, occurredAt);
        closingStartedAt = occurredAt;
        closingStartedBy = normalizedActor;
    }

    public void completeEod(String actor, LocalDateTime occurredAt) {
        String normalizedActor = transitionTo(EodState.CLOSED, actor, occurredAt);
        closedAt = occurredAt;
        closedBy = normalizedActor;
    }

    public void completeBod(String actor, LocalDateTime occurredAt) {
        String normalizedActor = transitionTo(EodState.OPEN, actor, occurredAt);
        openedAt = occurredAt;
        openedBy = normalizedActor;
    }

    private void initialize(
            LocalDate businessDate,
            EodState initialState,
            String actor,
            LocalDateTime occurredAt) {
        this.businessDate = Objects.requireNonNull(businessDate, "businessDate must not be null");
        this.state = Objects.requireNonNull(initialState, "initialState must not be null");
        this.createdAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.createdBy = actor;
        this.updatedAt = occurredAt;
        this.updatedBy = actor;
    }

    private String transitionTo(EodState target, String actor, LocalDateTime occurredAt) {
        String normalizedActor = requireActor(actor);
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (!state.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot transition business date " + businessDate + " from " + state + " to " + target);
        }
        state = target;
        updatedAt = occurredAt;
        updatedBy = normalizedActor;
        return normalizedActor;
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        String normalized = actor.trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("actor must not exceed 80 characters");
        }
        return normalized;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public EodState getState() {
        return state;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getPreparedAt() {
        return preparedAt;
    }

    public String getPreparedBy() {
        return preparedBy;
    }

    public LocalDateTime getClosingStartedAt() {
        return closingStartedAt;
    }

    public String getClosingStartedBy() {
        return closingStartedBy;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public LocalDateTime getBodStartedAt() {
        return bodStartedAt;
    }

    public String getBodStartedBy() {
        return bodStartedBy;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public String getOpenedBy() {
        return openedBy;
    }
}
