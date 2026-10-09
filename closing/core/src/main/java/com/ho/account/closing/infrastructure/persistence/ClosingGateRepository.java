package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingGateRepository extends JpaRepository<ClosingGateEntity, Long> {
    List<ClosingGateEntity> findByClosingCalendar(ClosingCalendarEntity closingCalendar);
}
