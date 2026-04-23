package com.ho.account.loan.application.port.out;

import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import com.ho.account.loan.domain.LoanContract;
import java.util.List;

/**
 * 대출 도메인을 위한 영속성 계층 인터페이스
 */
public interface LoanPort {
    void saveLoanContract(LoanContract loanContract);
    
    /**
     * 대용량 상각 스케줄 고속 저장
     */
    void saveAllAmortizationEntries(List<LoanAmortizationScheduleEntry> entries);
    
    LoanContract findByContractNo(String contractNo);
}
