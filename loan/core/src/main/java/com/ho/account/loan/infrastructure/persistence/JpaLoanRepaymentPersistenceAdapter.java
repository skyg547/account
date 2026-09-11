package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.application.port.out.LoanRepaymentPersistencePort;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaLoanRepaymentPersistenceAdapter implements LoanRepaymentPersistencePort {
    private final LoanEventRepository eventRepository;
    private final LoanRepository loanRepository;

    @Override
    public Optional<LoanEvent> findRepayment(Long loanId, LocalDate date) {
        var events = eventRepository.findByLoanIdAndEventDateAndEventTypeIn(loanId, date,
                List.of(LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING,
                        LoanEvent.EventType.SCHEDULED_REPAYMENT));
        if (events.size() > 1) {
            throw new IllegalStateException("Duplicate scheduled repayment events require reconciliation.");
        }
        return events.stream().findFirst();
    }

    @Override
    public boolean hasPendingRepayment(Long loanId) {
        return eventRepository.existsByLoanIdAndEventType(
                loanId, LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING);
    }

    @Override public LoanEvent saveEvent(LoanEvent event) { return eventRepository.save(event); }
    @Override public Loan saveLoan(Loan loan) { return loanRepository.save(loan); }
}
