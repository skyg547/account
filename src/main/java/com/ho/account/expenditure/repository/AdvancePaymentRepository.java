package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.AdvancePaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, Long> {
    List<AdvancePayment> findByVendorBusinessPartnerCodeAndStatus(String vendorCode, AdvancePaymentStatus status);
    List<AdvancePayment> findByVendorBusinessPartnerCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);
}
