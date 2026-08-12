package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.PaymentStatus;
import com.ho.account.expenditure.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {
    List<PaymentJpaEntity> findByPaymentRunId(Long paymentRunId);
    List<PaymentJpaEntity> findByVendorCodeAndStatus(String vendorCode, PaymentStatus status);
}
