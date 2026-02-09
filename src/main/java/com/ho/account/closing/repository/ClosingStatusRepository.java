package com.accounting.system.closing.repository;

import com.accounting.system.closing.domain.ClosingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClosingStatusRepository extends JpaRepository<ClosingStatus, String> {
    Optional<ClosingStatus> findByYearMonth(String yearMonth);
}
