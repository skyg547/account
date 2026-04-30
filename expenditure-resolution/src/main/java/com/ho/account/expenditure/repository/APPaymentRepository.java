package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.APPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface APPaymentRepository extends JpaRepository<APPayment, Long> {
    List<APPayment> findByExpenditureResolutionId(Long expenditureResolutionId);
}
