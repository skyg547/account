package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.application.port.out.LoanAccrualPersistencePort;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaLoanAccrualPersistenceAdapter implements LoanAccrualPersistencePort {

    private final LoanRepository loanRepository;
    private final EIRAmortizationScheduleRepository scheduleRepository;
    private final LoanAccrualLogRepository accrualLogRepository;

    @Override
    public Optional<Loan> findLoanForUpdate(Long loanId) {
        return loanRepository.findByIdForUpdate(loanId);
    }

    @Override
    public Optional<EIRAmortizationSchedule> findSchedule(Long loanId, LocalDate accrualDate) {
        return scheduleRepository.findByLoanIdAndScheduleDate(loanId, accrualDate);
    }

    @Override
    public Optional<LoanAccrualLog> findAccrualLog(Long loanId, LocalDate accrualDate) {
        return accrualLogRepository.findByLoanIdAndAccrualDate(loanId, accrualDate);
    }

    @Override
    public LoanAccrualLog saveAccrualLog(LoanAccrualLog log) {
        return accrualLogRepository.save(log);
    }
}
