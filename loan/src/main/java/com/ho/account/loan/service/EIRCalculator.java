package com.ho.account.loan.service;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanContract;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.List;

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
     * @param loan 대출 정보 (원금, 명목이자율 정보 포함)
     * @param deferredItems 이연 항목 목록 (수수료, 비용 등)
     * @return 계산된 유효이자율 (연율, %)
     */
    public BigDecimal calculateEIR(Loan loan, List<DeferredItem> deferredItems) {
        BigDecimal principal = loan.getPrincipalAmount();
        
        // 이연 항목 합계 계산 (수익은 +, 비용은 -로 처리되어야 함)
        // DeferredItemType의 성격에 따라 부호를 결정해야 하지만, 여기서는 단순 합산으로 가정
        BigDecimal fees = deferredItems.stream()
                .map(DeferredItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 순 현금 유출액 (투자액) = 원금 - 수수료 (Fee가 수익이면 투자금이 줄어듦)
        double initialInvestment = principal.subtract(fees).doubleValue();

        // 총 회차 (개월 수)
        int totalPeriods = Period.between(loan.getDisbursalDate(), loan.getMaturityDate()).getYears() * 12 
                         + Period.between(loan.getDisbursalDate(), loan.getMaturityDate()).getMonths();
        
        if (totalPeriods <= 0) return loan.getInterestRate();

        // 원리금 균등 상환액 (PMT) 계산 (명목 이자율 기반)
        double nominalRate = loan.getInterestRate().doubleValue() / 12.0; // 월 이자율 (0.05 / 12)
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

        return loan.getInterestRate().multiply(BigDecimal.valueOf(100)); // 수렴 실패 시 명목 이자율 반환
    }

    public BigDecimal calculateEIR(LoanContract loanContract, int totalPeriods) {
        BigDecimal principal = loanContract.getPrincipalAmount();
        BigDecimal fees = loanContract.getDeferredLoanFee() != null ? loanContract.getDeferredLoanFee() : BigDecimal.ZERO;

        double initialInvestment = principal.subtract(fees).doubleValue();
        if (totalPeriods <= 0) {
            return loanContract.getInterestRate().setScale(4, RoundingMode.HALF_UP);
        }

        double nominalRate = loanContract.getInterestRate().doubleValue() / 100.0 / 12.0;
        if (nominalRate == 0.0d) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        double pmt = (principal.doubleValue() * nominalRate) / (1 - Math.pow(1 + nominalRate, -totalPeriods));
        double r = nominalRate;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double f = 0;
            double df = 0;

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

        return loanContract.getInterestRate().setScale(4, RoundingMode.HALF_UP);
    }
}
