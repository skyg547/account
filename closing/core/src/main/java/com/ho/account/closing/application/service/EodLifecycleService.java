package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.EodLifecycleUseCase;
import com.ho.account.closing.application.port.out.DailyClosingStatusPersistencePort;
import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.domain.EodState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Service
@Transactional
public class EodLifecycleService implements EodLifecycleUseCase {

    private final DailyClosingStatusPersistencePort persistencePort;
    private final Clock clock;

    @Autowired
    public EodLifecycleService(DailyClosingStatusPersistencePort persistencePort) {
        this(persistencePort, Clock.systemUTC());
    }

    EodLifecycleService(DailyClosingStatusPersistencePort persistencePort, Clock clock) {
        this.persistencePort = Objects.requireNonNull(persistencePort, "persistencePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null").withZone(ZoneOffset.UTC);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyClosingStatus findStatus(LocalDate businessDate) {
        requireDate(businessDate, "businessDate");
        return persistencePort.findByBusinessDate(businessDate)
                .orElseThrow(() -> missing(businessDate));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DailyClosingStatus bootstrap(LocalDate businessDate, String actor) {
        requireDate(businessDate, "businessDate");
        requireActor(actor);
        return persistencePort.findLatestForUpdate()
                .map(existing -> reuseBootstrapOrFail(existing, businessDate))
                .orElseGet(() -> persistencePort.save(
                        DailyClosingStatus.bootstrap(businessDate, actor, now())));
    }

    @Override
    public DailyClosingStatus prepareEod(LocalDate businessDate, String actor) {
        return transition(
                businessDate,
                actor,
                EodState.PRE_CLOSING,
                status -> true,
                (status, context) -> status.prepareEod(context.actor(), context.occurredAt()));
    }

    @Override
    public DailyClosingStatus cancelEodPreparation(LocalDate businessDate, String actor) {
        return transition(
                businessDate,
                actor,
                EodState.OPEN,
                status -> status.getPreparedAt() != null
                        && status.getOpenedAt() != null,
                (status, context) -> status.cancelEodPreparation(context.actor(), context.occurredAt()));
    }

    @Override
    public DailyClosingStatus startEod(LocalDate businessDate, String actor) {
        return transition(
                businessDate,
                actor,
                EodState.CLOSING_IN_PROGRESS,
                status -> true,
                (status, context) -> status.startEod(context.actor(), context.occurredAt()));
    }

    @Override
    public DailyClosingStatus completeEod(LocalDate businessDate, String actor) {
        return transition(
                businessDate,
                actor,
                EodState.CLOSED,
                status -> true,
                (status, context) -> status.completeEod(context.actor(), context.occurredAt()));
    }

    @Override
    public DailyClosingStatus startBod(
            LocalDate closedDate,
            LocalDate nextBusinessDate,
            String actor) {
        requireDate(closedDate, "closedDate");
        requireDate(nextBusinessDate, "nextBusinessDate");
        requireActor(actor);
        if (!nextBusinessDate.isAfter(closedDate)) {
            throw new IllegalArgumentException("nextBusinessDate must be after closedDate");
        }

        DailyClosingStatus prior = persistencePort.findByBusinessDateForUpdate(closedDate)
                .orElseThrow(() -> missing(closedDate));
        if (prior.getState() != EodState.CLOSED) {
            throw new IllegalStateException(
                    "BOD requires prior business date " + closedDate + " to be CLOSED");
        }

        DailyClosingStatus latest = persistencePort.findLatestForUpdate()
                .orElseThrow(() -> new IllegalStateException("Closing status history disappeared during BOD"));
        if (latest.getBusinessDate().equals(nextBusinessDate)) {
            DailyClosingStatus actualPredecessor = persistencePort
                    .findPreviousForUpdate(nextBusinessDate)
                    .orElseThrow(() -> new IllegalStateException(
                            "Business date " + nextBusinessDate + " has no predecessor"));
            if (!actualPredecessor.getBusinessDate().equals(closedDate)
                    || actualPredecessor.getState() != EodState.CLOSED) {
                throw new IllegalStateException(
                        "Business date " + closedDate
                                + " is not the direct CLOSED predecessor of " + nextBusinessDate);
            }
            if (latest.getState() == EodState.BOD_IN_PROGRESS) {
                return latest;
            }
            throw new IllegalStateException(
                    "Business date " + nextBusinessDate + " already exists in state " + latest.getState());
        }
        if (!latest.getBusinessDate().equals(closedDate)) {
            throw new IllegalStateException(
                    "Prior business date " + closedDate + " is not the latest closing status");
        }

        return persistencePort.save(
                DailyClosingStatus.beginBusinessDay(nextBusinessDate, actor, now()));
    }

    @Override
    public DailyClosingStatus completeBod(LocalDate businessDate, String actor) {
        return transition(
                businessDate,
                actor,
                EodState.OPEN,
                status -> status.getBodStartedAt() != null
                        && status.getOpenedAt() != null,
                (status, context) -> status.completeBod(context.actor(), context.occurredAt()));
    }

    private DailyClosingStatus transition(
            LocalDate businessDate,
            String actor,
            EodState desiredState,
            Predicate<DailyClosingStatus> isIdempotentRetry,
            BiConsumer<DailyClosingStatus, TransitionContext> command) {
        requireDate(businessDate, "businessDate");
        String normalizedActor = requireActor(actor);
        DailyClosingStatus status = persistencePort.findByBusinessDateForUpdate(businessDate)
                .orElseThrow(() -> missing(businessDate));
        if (status.getState() == desiredState) {
            if (isIdempotentRetry.test(status)) {
                return status;
            }
            throw new IllegalStateException(
                    "Business date " + businessDate + " is already " + desiredState
                            + " without the requested transition lineage");
        }
        command.accept(status, new TransitionContext(normalizedActor, now()));
        return persistencePort.save(status);
    }

    private DailyClosingStatus reuseBootstrapOrFail(
            DailyClosingStatus existing,
            LocalDate requestedDate) {
        if (existing.getBusinessDate().equals(requestedDate)
                && existing.getState() == EodState.OPEN
                && existing.getBodStartedAt() == null
                && existing.getPreparedAt() == null
                && existing.getCreatedAt().equals(existing.getOpenedAt())) {
            return existing;
        }
        throw new IllegalStateException(
                "Bootstrap is allowed only when no daily closing status exists");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static LocalDate requireDate(LocalDate date, String name) {
        if (date == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return date;
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

    private static NoSuchElementException missing(LocalDate businessDate) {
        return new NoSuchElementException(
                "Daily closing status not found for business date " + businessDate);
    }

    private record TransitionContext(String actor, LocalDateTime occurredAt) {
    }
}
