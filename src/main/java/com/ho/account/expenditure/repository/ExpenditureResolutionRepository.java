package com.accounting.system.expenditure.repository;

import com.accounting.system.expenditure.domain.ExpenditureResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenditureResolutionRepository extends JpaRepository<ExpenditureResolution, Long> {
    Optional<ExpenditureResolution> findByResolutionNo(String resolutionNo);
    List<ExpenditureResolution> findByResolutionDateBetween(LocalDate startDate, LocalDate endDate);
    List<ExpenditureResolution> findByResolutionDate(LocalDate date);
}
