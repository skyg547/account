package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.RecalculationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecalculationRunRepository extends JpaRepository<RecalculationRun, Long> {
}
