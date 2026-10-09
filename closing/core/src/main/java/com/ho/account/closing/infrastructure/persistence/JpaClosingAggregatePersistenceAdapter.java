package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingAggregatePersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ReopenApproval;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaClosingAggregatePersistenceAdapter implements ClosingAggregatePersistencePort {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ClosingCalendar> lockCalendar(Long calendarId) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.id = :id", ClosingCalendarEntity.class)
                .setParameter("id", calendarId));
    }

    @Override
    public Optional<ClosingCalendar> lockCalendar(String fiscalYear, String fiscalPeriod) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.fiscalYear = :year and c.fiscalPeriod = :period", ClosingCalendarEntity.class)
                .setParameter("year", fiscalYear).setParameter("period", fiscalPeriod));
    }

    @Override
    public Optional<ClosingCalendar> lockCalendarForTask(Long taskId) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.id = (select t.closingCalendar.id from ClosingTask t where t.id = :id)", ClosingCalendarEntity.class)
                .setParameter("id", taskId));
    }

    @Override
    public Optional<ClosingCalendar> lockCalendarForGate(Long gateId) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.id = (select g.closingCalendar.id from ClosingGate g where g.id = :id)", ClosingCalendarEntity.class)
                .setParameter("id", gateId));
    }

    @Override
    public Optional<Long> findApprovalFiscalPeriodId(Long approvalId) {
        // Resolve the immutable aggregate key without adding a possibly stale approval to the context.
        return entityManager.createQuery("select a.fiscalPeriodId from ReopenApproval a where a.id = :id", Long.class)
                .setParameter("id", approvalId).getResultStream().findFirst();
    }

    @Override
    public Optional<ClosingTask> refreshTask(Long taskId) {
        return refresh(ClosingTaskEntity.class, taskId).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public Optional<ClosingGate> refreshGate(Long gateId) {
        return refresh(ClosingGateEntity.class, gateId).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public Optional<ReopenApproval> refreshApproval(Long approvalId) {
        return refresh(ReopenApprovalEntity.class, approvalId).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public List<ClosingTask> refreshTasks(ClosingCalendar calendar) {
        List<ClosingTaskEntity> tasks = entityManager.createQuery("select t from ClosingTask t where t.closingCalendar.id = :calendarId", ClosingTaskEntity.class)
                .setParameter("calendarId", calendar.getId()).getResultList();
        tasks.forEach(entityManager::refresh);
        return tasks.stream().map(ClosingEntityMapper::toDomain).toList();
    }

    @Override
    public List<ClosingGate> refreshGates(ClosingCalendar calendar) {
        List<ClosingGateEntity> gates = entityManager.createQuery("select g from ClosingGate g where g.closingCalendar.id = :calendarId", ClosingGateEntity.class)
                .setParameter("calendarId", calendar.getId()).getResultList();
        gates.forEach(entityManager::refresh);
        return gates.stream().map(ClosingEntityMapper::toDomain).toList();
    }

    private Optional<ClosingCalendar> lock(TypedQuery<ClosingCalendarEntity> query) {
        // A lock query can return an older managed snapshot; refresh after acquiring the row lock.
        Optional<ClosingCalendarEntity> result = query.setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst();
        result.ifPresent(calendar -> entityManager.refresh(calendar, LockModeType.PESSIMISTIC_WRITE));
        return result.map(ClosingEntityMapper::toDomain);
    }

    private <T> Optional<T> refresh(Class<T> type, Long id) {
        // Checklist and approval rows may already be managed before another transaction commits.
        T entity = entityManager.find(type, id);
        if (entity != null) entityManager.refresh(entity);
        return Optional.ofNullable(entity);
    }
}
