package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.ExpenditureResolution;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenditureResolutionPersistencePort {
    ExpenditureResolution save(ExpenditureResolution resolution);
    Optional<ExpenditureResolution> findById(Long id);
    List<ExpenditureResolution> findByResolutionDate(LocalDate date);
    List<ExpenditureResolution> findByResolutionDateBetween(LocalDate startDate, LocalDate endDate);
}
