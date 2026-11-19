package com.ho.account.asset.infrastructure.persistence.repository;

import com.ho.account.asset.infrastructure.persistence.entity.LeaseRemeasurementReceiptEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseRemeasurementReceiptRepository extends JpaRepository<LeaseRemeasurementReceiptEntity, Long> {
    Optional<LeaseRemeasurementReceiptEntity> findByContractNoAndOperationId(String contractNo, String operationId);
}
