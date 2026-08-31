package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.infrastructure.persistence.entity.PaymentRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PaymentRunRepository extends JpaRepository<PaymentRunJpaEntity, Long> {

    @Query("SELECT p FROM PaymentRunJpaEntity p WHERE p.runDate = :runDate AND ((:description IS NULL AND p.description IS NULL) OR p.description = :description) AND p.createdBy = :createdBy ORDER BY p.id DESC")
    List<PaymentRunJpaEntity> findMatchingPaymentRuns(
            @Param("runDate") LocalDate runDate,
            @Param("description") String description,
            @Param("createdBy") String createdBy);
}
