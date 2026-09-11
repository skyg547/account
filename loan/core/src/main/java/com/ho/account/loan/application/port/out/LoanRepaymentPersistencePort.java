package com.ho.account.loan.application.port.out;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanEvent;
import java.time.LocalDate;
import java.util.Optional;

/** Call event methods only while holding the owning loan's write lock. */
public interface LoanRepaymentPersistencePort {
    Optional<LoanEvent> findRepayment(Long loanId, LocalDate date);
    boolean hasPendingRepayment(Long loanId);
    LoanEvent saveEvent(LoanEvent event);
    Loan saveLoan(Loan loan);
}
