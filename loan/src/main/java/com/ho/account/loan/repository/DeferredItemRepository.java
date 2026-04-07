package com.ho.account.loan.repository;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DeferredItem 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface DeferredItemRepository extends JpaRepository<DeferredItem, Long> {
    List<DeferredItem> findByLoan(Loan loan);
}
