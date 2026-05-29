package com.ho.account.ecl.core.domain.calculator;

import com.ho.account.shared.finance.enums.CrStaging;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * IFRS 9 기대신용손실(ECL) 전용 계산기.
 */
@Component
public class IfrsEclCalculator {

    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);

    public BigDecimal calculateEcl(
            CrStaging stage,
            List<BigDecimal> marginalPds,
            BigDecimal lgd,
            BigDecimal ead,
            BigDecimal discountRate) {

        if (marginalPds == null || lgd == null || ead == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalEcl = BigDecimal.ZERO;
        double discount = discountRate != null ? discountRate.doubleValue() : 0.05d;

        if (stage == CrStaging.STAGE1) {
            BigDecimal twelveMonthPd = marginalPds.isEmpty() ? BigDecimal.ZERO : marginalPds.get(0);
            BigDecimal discountFactor = BigDecimal.valueOf(1.0d / (1.0d + discount));
            return twelveMonthPd.multiply(lgd, MC)
                    .multiply(ead, MC)
                    .multiply(discountFactor, MC)
                    .setScale(4, RoundingMode.HALF_UP);
        }

        for (int t = 0; t < marginalPds.size(); t++) {
            BigDecimal marginalPd = marginalPds.get(t);
            BigDecimal discountFactor = BigDecimal.valueOf(1.0d / Math.pow(1.0d + discount, t + 1.0d));
            totalEcl = totalEcl.add(marginalPd.multiply(lgd, MC)
                    .multiply(ead, MC)
                    .multiply(discountFactor, MC));
        }

        return totalEcl.setScale(4, RoundingMode.HALF_UP);
    }
}
