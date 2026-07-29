package com.ho.account.closing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root for the lifecycle of a single business date.
 */
@Entity
@Table(name = "daily_closing_status")
public class DailyClosingStatus {

    @Id
    @Column(name = "date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private EodState state;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "prepared_by", length = 80)
    private String preparedBy;

    @Column(name = "closing_started_at")
    private LocalDateTime closingStartedAt;

    @Column(name = "closing_started_by", length = 80)
    private String closingStartedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 80)
    private String closedBy;

    @Column(name = "bod_started_at")
    private LocalDateTime bodStartedAt;

    @Column(name = "bod_started_by", length = 80)
    private String bodStartedBy;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "opened_by", length = 80)
    private String openedBy;

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
