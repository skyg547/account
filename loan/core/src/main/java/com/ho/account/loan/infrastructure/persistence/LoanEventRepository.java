package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.LoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanEventRepository extends JpaRepository<LoanEvent, Long> {
    boolean existsByLoanIdAndEventType(Long loanId, LoanEvent.EventType eventType);
    java.util.List<LoanEvent> findByLoanIdAndEventDateAndEventTypeIn(
            Long loanId, java.time.LocalDate date, java.util.Collection<LoanEvent.EventType> types);
}
