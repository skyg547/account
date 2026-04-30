package com.ho.account.risk.infrastructure.persistence;

import com.ho.account.risk.domain.CreditRiskExposure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditRiskExposureRepository extends JpaRepository<CreditRiskExposure, Long> {
    List<CreditRiskExposure> findByBaseDate(LocalDate baseDate);
    Optional<CreditRiskExposure> findBySourceSystemIdAndSourceReferenceIdAndBaseDate(
            String sourceSystemId, String sourceReferenceId, LocalDate baseDate);
}
