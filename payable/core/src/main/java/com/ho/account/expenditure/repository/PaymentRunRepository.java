package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.infrastructure.persistence.entity.PaymentRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRunRepository extends JpaRepository<PaymentRunJpaEntity, Long> {
}
