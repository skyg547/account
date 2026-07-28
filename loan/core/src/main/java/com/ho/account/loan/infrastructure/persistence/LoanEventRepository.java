package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.LoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanEventRepository extends JpaRepository<LoanEvent, Long> {
}
