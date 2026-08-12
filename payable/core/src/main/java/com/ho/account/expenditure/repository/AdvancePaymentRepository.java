package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.AdvancePaymentStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.AdvancePaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdvancePaymentRepository extends JpaRepository<AdvancePaymentJpaEntity, Long> {
    List<AdvancePaymentJpaEntity> findByVendorCodeAndStatus(String vendorCode, AdvancePaymentStatus status);
    List<AdvancePaymentJpaEntity> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);
}
