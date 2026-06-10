package com.ho.account.loan.application.port.out;

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

/**
 * 대출 애플리케이션 서비스가 사용하는 영속성 출력 포트.
 *
 * <p>업무 서비스는 저장 기술이나 Spring Data JPA 인터페이스를 모르고, 대출 aggregate 저장 의도만
 * 표현합니다. 실제 repository 조합은 infrastructure adapter가 담당합니다.
 */
public interface LoanPersistencePort {

    Loan saveLoan(Loan loan);

    Optional<Loan> findLoan(Long id);

    LoanDisbursal saveDisbursal(LoanDisbursal disbursal);

    Optional<DeferredItemType> findDeferredItemType(Long id);

    Optional<DeferredItemType> findDeferredItemType(String code);

    DeferredItemType saveDeferredItemType(DeferredItemType itemType);

    DeferredItem saveDeferredItem(DeferredItem deferredItem);

    List<DeferredItem> findDeferredItems(Loan loan);

    void deleteSchedules(List<EIRAmortizationSchedule> schedules);

    List<EIRAmortizationSchedule> findSchedules(Loan loan);

    List<EIRAmortizationSchedule> findSchedulesFrom(Loan loan, LocalDate startDate);

    EIRAmortizationSchedule saveSchedule(EIRAmortizationSchedule schedule);

    List<EIRAmortizationSchedule> findSchedulesOrdered(Loan loan);

    RecalculationRun saveRecalculationRun(RecalculationRun run);

    LoanEvent saveLoanEvent(LoanEvent event);
}
