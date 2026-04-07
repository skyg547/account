package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.PaymentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRunRepository extends JpaRepository<PaymentRun, Long> {
}
