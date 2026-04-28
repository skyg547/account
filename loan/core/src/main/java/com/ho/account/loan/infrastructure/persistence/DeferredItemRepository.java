package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DeferredItem ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface DeferredItemRepository extends JpaRepository<DeferredItem, Long> {
    List<DeferredItem> findByLoan(Loan loan);
}
