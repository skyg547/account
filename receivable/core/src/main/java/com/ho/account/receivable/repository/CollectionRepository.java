package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<CollectionJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CollectionJpaEntity c where c.id = :id")
    Optional<CollectionJpaEntity> findByIdForUpdate(@Param("id") Long id);
    List<CollectionJpaEntity> findByStatus(CollectionStatus status);
    List<CollectionJpaEntity> findByStatusIn(List<CollectionStatus> statuses);
    Optional<CollectionJpaEntity> findByReferenceNo(String referenceNo);
    List<CollectionJpaEntity> findByCustomerCodeAndStatus(String customerCode, CollectionStatus status);
    List<CollectionJpaEntity> findByCustomerCodeAndAmountBetween(String customerCode, BigDecimal minAmount, BigDecimal maxAmount);
}
