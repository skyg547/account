package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingTask;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaClosingTaskPersistenceAdapter implements ClosingTaskPersistencePort {
    private final ClosingTaskRepository repository;
    private final ClosingCalendarRepository calendarRepository;

    @Override
    public ClosingTask save(ClosingTask value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(attach(ClosingEntityMapper.toEntity(value))));
    }

    @Override
    public Optional<ClosingTask> findById(Long id) {
        return repository.findById(id).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public List<ClosingTask> findByClosingCalendar(ClosingCalendar calendar) {
        return repository.findByClosingCalendarOrderByTaskOrderAsc(
                calendarRepository.getReferenceById(calendar.getId())).stream()
                .map(ClosingEntityMapper::toDomain).toList();
    }

    @Override
    public List<ClosingTask> findByClosingCalendarId(Long calendarId) {
        return repository.findByClosingCalendarId(calendarId).stream()
                .map(ClosingEntityMapper::toDomain).toList();
    }

    private ClosingTaskEntity attach(ClosingTaskEntity entity) {
        entity.setClosingCalendar(calendarRepository.getReferenceById(entity.getClosingCalendar().getId()));
        return entity;
    }
}
