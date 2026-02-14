package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaymentRunId(Long paymentRunId);
    List<Payment> findByVendorBusinessPartnerCodeAndStatus(String vendorCode, PaymentStatus status);
}
