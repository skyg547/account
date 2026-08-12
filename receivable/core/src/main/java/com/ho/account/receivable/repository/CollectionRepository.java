package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<CollectionJpaEntity, Long> {
    List<CollectionJpaEntity> findByStatus(CollectionStatus status);
    List<CollectionJpaEntity> findByStatusIn(List<CollectionStatus> statuses);
    Optional<CollectionJpaEntity> findByReferenceNo(String referenceNo);
    List<CollectionJpaEntity> findByCustomerCodeAndStatus(String customerCode, CollectionStatus status);
    List<CollectionJpaEntity> findByCustomerCodeAndAmountBetween(String customerCode, BigDecimal minAmount, BigDecimal maxAmount);
}
