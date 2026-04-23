package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.LoanContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanContractRepository extends JpaRepository<LoanContract, Long> {
    Optional<LoanContract> findByLoanContractNo(String loanContractNo);

    boolean existsByLoanContractNo(String loanContractNo);

    java.util.List<LoanContract> findByStatus(String status);
}
