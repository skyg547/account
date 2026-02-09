package com.ho.account.closing.repository;

import com.ho.account.closing.domain.ClosingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClosingStatusRepository extends JpaRepository<ClosingStatus, String> {
    Optional<ClosingStatus> findByYearMonth(String yearMonth);
}
