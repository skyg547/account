package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import java.util.List;
import java.util.Optional;

public interface ClosingGatePersistencePort {
    ClosingGate save(ClosingGate closingGate);
    Optional<ClosingGate> findById(Long id);
    List<ClosingGate> findByClosingCalendar(ClosingCalendar closingCalendar);
}
