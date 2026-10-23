package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.AdvancePaymentStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.AdvancePaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdvancePaymentRepository extends JpaRepository<AdvancePaymentJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdvancePaymentJpaEntity a WHERE a.id = :id")
    Optional<AdvancePaymentJpaEntity> findByIdForUpdate(@Param("id") Long id);
    List<AdvancePaymentJpaEntity> findByVendorCodeAndStatus(String vendorCode, AdvancePaymentStatus status);
    List<AdvancePaymentJpaEntity> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);
}
