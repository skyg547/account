package com.ho.account.risk.infrastructure.persistence;

import com.ho.account.risk.domain.RiskParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RiskParameterRepository extends JpaRepository<RiskParameter, Long> {
    
    @Query("SELECT r FROM RiskParameter r WHERE r.parameterCategory = :category " +
           "AND r.assetClass = :assetClass AND r.approachType = :approach " +
           "AND :baseDate BETWEEN r.validFrom AND r.validTo")
    Optional<RiskParameter> findActiveParameter(
            @Param("category") String category,
            @Param("assetClass") String assetClass,
            @Param("approach") String approach,
            @Param("baseDate") LocalDate baseDate);
}
