package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

/**
 * Loan ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> findByIdForUpdate(Long id);
}
