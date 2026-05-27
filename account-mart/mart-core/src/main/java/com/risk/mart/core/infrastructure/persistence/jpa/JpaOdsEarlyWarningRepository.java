package com.risk.mart.core.infrastructure.persistence.jpa;

import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsEarlyWarningEntity;
import com.risk.mart.core.infrastructure.persistence.entity.ods.OdsEarlyWarningId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JpaOdsEarlyWarningRepository extends JpaRepository<OdsEarlyWarningEntity, OdsEarlyWarningId> {
    Optional<OdsEarlyWarningEntity> findTopByCustomerCodeOrderByBaseDateDesc(String customerCode);

    Optional<OdsEarlyWarningEntity> findTopByCustomerCodeAndBaseDateLessThanEqualOrderByBaseDateDesc(
            String customerCode,
            LocalDate baseDate);
}
