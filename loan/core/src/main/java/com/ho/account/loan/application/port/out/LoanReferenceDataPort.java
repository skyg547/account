package com.ho.account.loan.application.port.out;

import com.ho.account.loan.domain.Loan;

/**
 * 대출에서 필요한 거래처·통화·계정 기준정보 확인 포트.
 */
public interface LoanReferenceDataPort {

    /**
     * 요청 Loan에 담긴 거래처/통화 식별자를 검증하고 영속 가능한 참조를 연결합니다.
     */
    void attachValidatedLoanReferences(Loan loan);

    /**
     * 자동분개에 사용할 계정 코드가 실제 활성 계정인지 확인합니다.
     */
    String requireAccountCode(String accountCode);
}
