package com.ho.account.loan.application.pipeline;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.service.InterestAccrualService;
import com.ho.account.loan.service.InterestAccrualService.AccrualResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Batch chunk로 전달된 대출을 core 업무 규칙에 따라 처리하는 대량 변환 파이프라인입니다.
 *
 * <p>Batch 모듈은 chunk/reader/writer 흐름만 구성하고, 반복 처리와 실패 집계는 이 core 파이프라인이
 * 담당합니다. 성공 로그는 멱등 키로 재실행 시 건너뛰고, 실패 로그는 다음 재실행에서 다시 시도됩니다.</p>
 */
@Component
@RequiredArgsConstructor
public class LoanInterestAccrualPipeline {

    private final InterestAccrualService interestAccrualService;

    public void processChunk(List<? extends Loan> loans, LocalDate accrualDate) {
        if (loans == null) {
            throw new IllegalArgumentException("loans chunk is required.");
        }
        if (accrualDate == null) {
            throw new IllegalArgumentException("accrualDate is required.");
        }

        List<Long> failedLoanIds = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan == null || loan.getId() == null) {
                throw new IllegalArgumentException("Every chunk item must be a persisted loan.");
            }
            AccrualResult result = interestAccrualService.processIndividualAccrual(loan.getId(), accrualDate);
            if (result == AccrualResult.FAILED) {
                failedLoanIds.add(loan.getId());
            }
        }
        if (!failedLoanIds.isEmpty()) {
            throw new IllegalStateException("Loan accrual failed for loanIds=" + failedLoanIds);
        }
    }
}
