package com.ho.account.loan.service;

import com.ho.account.loan.domain.LoanContract;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 대출 유효이자율(EIR) 계산기
 * Newton-Raphson 방식을 사용하여 내부수익률(IRR)을 계산합니다.
 */
@Component
public class EIRCalculator {

    private static final int MAX_ITERATIONS = 100;
    private static final double PRECISION = 1e-7;

    /**
     * 대출 계약의 EIR을 계산합니다.
     * 
     * @param loanContract 대출 계약 (원금, 기간, 이연수수료, 명목이자율 정보 포함)
     * @return 계산된 유효이자율 (연율, %)
     */
    public BigDecimal calculateEIR(LoanContract loanContract, int totalPeriods) {
        BigDecimal principal = loanContract.getPrincipalAmount();
        BigDecimal fees = loanContract.getDeferredLoanFee(); // 이연대출부대손익 (수익은 +, 비용은 -)

        // 순 현금 유출액 (투자액) = 원금 - 수수료 (Fee가 수익이면 투자금이 줄어듦)
        double initialInvestment = principal.subtract(fees).doubleValue();

        // 원리금 균등 상환액 (PMT) 계산 (명목 이자율 기반)
        double nominalRate = loanContract.getInterestRate().doubleValue() / 1200.0; // 월 이자율
        double pmt = (principal.doubleValue() * nominalRate) / (1 - Math.pow(1 + nominalRate, -totalPeriods));

        // IRR 계산 (월 이자율)
        double r = nominalRate; // 초기 추정치
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double f = 0;
            double df = 0;

            // f(r) = -I + sum(PMT / (1+r)^t)
            // df(r) = sum(-t * PMT / (1+r)^(t+1))
            for (int t = 1; t <= totalPeriods; t++) {
                double discountFactor = Math.pow(1 + r, t);
                f += pmt / discountFactor;
                df -= (t * pmt) / (discountFactor * (1 + r));
            }
            f -= initialInvestment;

            double nextR = r - f / df;
            if (Math.abs(nextR - r) < PRECISION) {
                return BigDecimal.valueOf(nextR * 1200).setScale(4, RoundingMode.HALF_UP);
            }
            r = nextR;
        }

        return loanContract.getInterestRate(); // 수렴 실패 시 명목 이자율 반환
    }
}
