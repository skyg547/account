package com.ho.account.closing.repository;

import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ClosingGate 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ClosingGateRepository extends JpaRepository<ClosingGate, Long> {
    List<ClosingGate> findByClosingCalendar(ClosingCalendar closingCalendar);
}
