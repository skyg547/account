package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaClosingGatePersistenceAdapter implements ClosingGatePersistencePort {
    private final ClosingGateRepository repository;
    private final ClosingCalendarRepository calendarRepository;

    @Override
    public ClosingGate save(ClosingGate value) {
        return ClosingEntityMapper.toDomain(
                repository.saveAndFlush(attach(ClosingEntityMapper.toEntity(value))));
    }

    @Override
    public Optional<ClosingGate> findById(Long id) {
        return repository.findById(id).map(ClosingEntityMapper::toDomain);
    }

    @Override
    public List<ClosingGate> findByClosingCalendar(ClosingCalendar calendar) {
        return repository.findByClosingCalendar(
                calendarRepository.getReferenceById(calendar.getId())).stream()
                .map(ClosingEntityMapper::toDomain).toList();
    }

    private ClosingGateEntity attach(ClosingGateEntity entity) {
        entity.setClosingCalendar(calendarRepository.getReferenceById(entity.getClosingCalendar().getId()));
        return entity;
    }
}
