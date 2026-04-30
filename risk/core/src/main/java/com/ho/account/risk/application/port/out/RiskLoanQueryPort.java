package com.ho.account.risk.application.port.out;

import com.ho.account.loan.domain.Loan;
import java.util.List;

/**
 * [RiskLoanQueryPort]
 * Risk 모듈에서 대출 정보를 조회하기 위한 아웃바운드 포트.
 * 타 모듈(Loan)의 데이터를 가져오기 위한 인터페이스입니다.
 */
public interface RiskLoanQueryPort {
    List<Loan> findAllActiveLoans();
}
