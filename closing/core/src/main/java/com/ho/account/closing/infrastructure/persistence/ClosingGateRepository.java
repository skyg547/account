package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingGateRepository extends JpaRepository<ClosingGate, Long>, ClosingGatePersistencePort {
    List<ClosingGate> findByClosingCalendar(ClosingCalendar closingCalendar);
}
