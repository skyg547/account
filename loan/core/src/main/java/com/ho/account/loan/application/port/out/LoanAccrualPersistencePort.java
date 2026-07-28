package com.ho.account.loan.application.port.out;

import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 이자 발생 유즈케이스가 사용하는 기술 독립 영속성 포트입니다.
 *
 * <p>Loan 행 잠금, 단일 EIR 스케줄 조회, 멱등 로그 저장의 구체적인 JPA 구현을 서비스에서 숨깁니다.</p>
 */
public interface LoanAccrualPersistencePort {

    Optional<Loan> findLoanForUpdate(Long loanId);

    Optional<EIRAmortizationSchedule> findSchedule(Long loanId, LocalDate accrualDate);

    Optional<LoanAccrualLog> findAccrualLog(Long loanId, LocalDate accrualDate);

    LoanAccrualLog saveAccrualLog(LoanAccrualLog log);
}
