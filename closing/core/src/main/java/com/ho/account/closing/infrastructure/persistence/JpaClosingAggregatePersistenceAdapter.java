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
import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaClosingAggregatePersistenceAdapter implements ClosingAggregatePersistencePort {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ClosingCalendar> lockCalendar(Long calendarId) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.id = :id", ClosingCalendar.class)
                .setParameter("id", calendarId));
    }

    @Override
    public Optional<ClosingCalendar> lockCalendar(String fiscalYear, String fiscalPeriod) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.fiscalYear = :year and c.fiscalPeriod = :period", ClosingCalendar.class)
                .setParameter("year", fiscalYear).setParameter("period", fiscalPeriod));
    }

    @Override
    public Optional<ClosingCalendar> lockCalendarForTask(Long taskId) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.id = (select t.closingCalendar.id from ClosingTask t where t.id = :id)", ClosingCalendar.class)
                .setParameter("id", taskId));
    }

    @Override
    public Optional<ClosingCalendar> lockCalendarForGate(Long gateId) {
        return lock(entityManager.createQuery("select c from ClosingCalendar c where c.id = (select g.closingCalendar.id from ClosingGate g where g.id = :id)", ClosingCalendar.class)
                .setParameter("id", gateId));
    }

    @Override
    public Optional<Long> findApprovalFiscalPeriodId(Long approvalId) {
        // A scalar lookup cannot introduce a stale managed approval into the persistence context.
        return entityManager.createQuery("select a.fiscalPeriodId from ReopenApproval a where a.id = :id", Long.class)
                .setParameter("id", approvalId).getResultStream().findFirst();
    }

    @Override
    public Optional<ClosingTask> refreshTask(Long taskId) {
        return refresh(ClosingTask.class, taskId);
    }

    @Override
    public Optional<ClosingGate> refreshGate(Long gateId) {
        return refresh(ClosingGate.class, gateId);
    }

    @Override
    public Optional<ReopenApproval> refreshApproval(Long approvalId) {
        return refresh(ReopenApproval.class, approvalId);
    }

    @Override
    public List<ClosingTask> refreshTasks(ClosingCalendar calendar) {
        List<ClosingTask> tasks = entityManager.createQuery("select t from ClosingTask t where t.closingCalendar = :calendar", ClosingTask.class)
                .setParameter("calendar", calendar).getResultList();
        tasks.forEach(entityManager::refresh);
        return tasks;
    }

    @Override
    public List<ClosingGate> refreshGates(ClosingCalendar calendar) {
        List<ClosingGate> gates = entityManager.createQuery("select g from ClosingGate g where g.closingCalendar = :calendar", ClosingGate.class)
                .setParameter("calendar", calendar).getResultList();
        gates.forEach(entityManager::refresh);
        return gates;
    }

    private Optional<ClosingCalendar> lock(TypedQuery<ClosingCalendar> query) {
        Optional<ClosingCalendar> result = query.setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst();
        // Lock queries may return a previously managed snapshot; refresh after obtaining the row lock.
        result.ifPresent(calendar -> entityManager.refresh(calendar, LockModeType.PESSIMISTIC_WRITE));
        return result;
    }

    private <T> Optional<T> refresh(Class<T> type, Long id) {
        T entity = entityManager.find(type, id);
        if (entity != null) {
            entityManager.refresh(entity);
        }
        return Optional.ofNullable(entity);
    }
}
