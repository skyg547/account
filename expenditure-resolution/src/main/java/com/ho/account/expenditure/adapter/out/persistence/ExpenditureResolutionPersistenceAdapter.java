package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.ExpenditureResolutionPersistencePort;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ExpenditureResolutionPersistenceAdapter implements ExpenditureResolutionPersistencePort {

    private final ExpenditureResolutionRepository repository;

    public ExpenditureResolutionPersistenceAdapter(ExpenditureResolutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExpenditureResolution save(ExpenditureResolution resolution) {
        return repository.save(resolution);
    }

    @Override
    public Optional<ExpenditureResolution> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ExpenditureResolution> findByResolutionDate(LocalDate date) {
        return repository.findByResolutionDate(date);
    }

    @Override
    public List<ExpenditureResolution> findByResolutionDateBetween(LocalDate startDate, LocalDate endDate) {
        return repository.findByResolutionDateBetween(startDate, endDate);
    }
}
