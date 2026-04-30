package com.ho.account.risk.infrastructure.external;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import com.ho.account.risk.application.port.out.RiskLoanQueryPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [RiskLoanQueryAdapter]
 * Loan 모듈의 Repository를 직접 참조하여 데이터를 가져오는 어댑터.
 * (추후 MSA 환경에서는 OpenFeign 등을 통한 API 호출로 변경될 수 있음)
 */
@Component
public class RiskLoanQueryAdapter implements RiskLoanQueryPort {

    private final LoanRepository loanRepository;

    public RiskLoanQueryAdapter(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public List<Loan> findAllActiveLoans() {
        return loanRepository.findByStatus(Loan.LoanStatus.ACTIVE);
    }
}
