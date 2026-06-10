package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanPersistenceAdapter implements LoanPersistencePort {

    private final LoanRepository loanRepository;
    private final LoanDisbursalRepository loanDisbursalRepository;
    private final LoanEventRepository loanEventRepository;
    private final DeferredItemTypeRepository deferredItemTypeRepository;
    private final DeferredItemRepository deferredItemRepository;
    private final EIRAmortizationScheduleRepository scheduleRepository;
    private final RecalculationRunRepository recalculationRunRepository;

    @Override public Loan saveLoan(Loan loan) { return loanRepository.save(loan); }
    @Override public Optional<Loan> findLoan(Long id) { return loanRepository.findById(id); }
    @Override public LoanDisbursal saveDisbursal(LoanDisbursal disbursal) { return loanDisbursalRepository.save(disbursal); }
    @Override public Optional<DeferredItemType> findDeferredItemType(Long id) { return deferredItemTypeRepository.findById(id); }
    @Override public Optional<DeferredItemType> findDeferredItemType(String code) { return deferredItemTypeRepository.findByCode(code); }
    @Override public DeferredItemType saveDeferredItemType(DeferredItemType itemType) { return deferredItemTypeRepository.save(itemType); }
    @Override public DeferredItem saveDeferredItem(DeferredItem deferredItem) { return deferredItemRepository.save(deferredItem); }
    @Override public List<DeferredItem> findDeferredItems(Loan loan) { return deferredItemRepository.findByLoan(loan); }
    @Override public void deleteSchedules(List<EIRAmortizationSchedule> schedules) { scheduleRepository.deleteAll(schedules); }
    @Override public List<EIRAmortizationSchedule> findSchedules(Loan loan) { return scheduleRepository.findByLoan(loan); }
    @Override public List<EIRAmortizationSchedule> findSchedulesFrom(Loan loan, LocalDate startDate) {
        return scheduleRepository.findByLoanAndScheduleDateGreaterThanEqualOrderByScheduleDateAsc(loan, startDate);
    }
    @Override public EIRAmortizationSchedule saveSchedule(EIRAmortizationSchedule schedule) { return scheduleRepository.save(schedule); }
    @Override public List<EIRAmortizationSchedule> findSchedulesOrdered(Loan loan) {
        return scheduleRepository.findByLoanOrderByScheduleDateAsc(loan);
    }
    @Override public RecalculationRun saveRecalculationRun(RecalculationRun run) { return recalculationRunRepository.save(run); }
    @Override public LoanEvent saveLoanEvent(LoanEvent event) { return loanEventRepository.save(event); }
}
