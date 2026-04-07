package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Added import

/**
 * DifferenceReasonCode 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface DifferenceReasonCodeRepository extends JpaRepository<DifferenceReasonCode, Long> {
    Optional<DifferenceReasonCode> findByCode(String code);
}
