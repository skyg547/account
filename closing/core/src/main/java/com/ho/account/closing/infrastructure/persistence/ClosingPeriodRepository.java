package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ClosingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClosingPeriodRepository extends JpaRepository<ClosingPeriod, Long> {
}
