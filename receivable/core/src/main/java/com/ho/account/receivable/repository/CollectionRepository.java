package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByStatus(CollectionStatus status);
    List<Collection> findByStatusIn(List<CollectionStatus> statuses);
    Optional<Collection> findByReferenceNo(String referenceNo);
    List<Collection> findByCustomerCodeAndStatus(String customerCode, CollectionStatus status);
    List<Collection> findByCustomerCodeAndAmountBetween(String customerCode, BigDecimal minAmount, BigDecimal maxAmount);
}
