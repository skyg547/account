package com.risk.credit.core.domain.calculator;

import com.risk.common.enums.CrStaging;
import com.risk.common.enums.CustomerType;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.stereotype.Component;

import java.util.List;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [The Brain] 신용 리스크 핵심 계산 엔진 (Credit Risk Engine v4.2)
 */
@Slf4j
@Component
public class CreditRiskCalculator {

    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);
    private static final BigDecimal MULTIPLIER_12_5 = new BigDecimal("12.5"); // RWA 산출 시 사용되는 자본승수 (8% 자본비율의 역수)
    private final Map<String, Double> correlationCache = new ConcurrentHashMap<>();

    /**
     * IFRS 9 기대신용손실 (Expected Credit Loss, ECL) 산출
     * 
     * 💡 [초보자를 위한 가이드]
     * 기대신용손실(ECL)이란, 쉽게 말해 부도가 날 가능성을 미리 계산해서 쌓아두는 '비상금'입니다.
     * 공식: ECL = PD(부도확률) * LGD(손실률) * EAD(노출금액)
     * - PD: 돈을 못 갚을 확률 (0~100%)
     * - LGD: 실제로 부도가 났을 때 담보 등을 빼고 최종적으로 못 받게 되는 비율
     * - EAD: 부도가 나는 시점에 빌려준 돈의 총액
     */
    public BigDecimal calculateEcl(
            CrStaging stage,
            List<BigDecimal> marginalPds,
            BigDecimal lgd,
            BigDecimal ead,
            BigDecimal discountRate) {

        if (marginalPds == null || lgd == null || ead == null) return BigDecimal.ZERO;

        BigDecimal totalEcl = BigDecimal.ZERO;
        double r = (discountRate != null) ? discountRate.doubleValue() : 0.05;

        if (stage == CrStaging.STAGE1) {
            BigDecimal pd12m = marginalPds.isEmpty() ? BigDecimal.ZERO : marginalPds.get(0);
            BigDecimal df = BigDecimal.valueOf(1.0 / (1.0 + r));
            return pd12m.multiply(lgd, MC).multiply(ead, MC).multiply(df, MC).setScale(4, RoundingMode.HALF_UP);
        }

        for (int t = 0; t < marginalPds.size(); t++) {
            BigDecimal mpd = marginalPds.get(t);
            BigDecimal df = BigDecimal.valueOf(1.0 / Math.pow(1.0 + r, t + 1.0));
            totalEcl = totalEcl.add(mpd.multiply(lgd, MC).multiply(ead, MC).multiply(df, MC));
        }

        return totalEcl.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 비예상 손실 (Unexpected Loss) 산출
     */
    public BigDecimal calculateUnexpectedLoss(BigDecimal pd, BigDecimal lgd, BigDecimal ead) {
        if (pd == null || lgd == null || ead == null) return BigDecimal.ZERO;
        double pdValue = pd.doubleValue();
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(Math.max(pdValue * (1 - pdValue), 0)));
        return ead.multiply(lgd, MC).multiply(stdDev, MC).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * [v4.2] 내부등급법(IRB) 위험가중자산(RWA) 산출
     * 
     * 💡 [초보자를 위한 가이드]
     * RWA(Risk Weighted Asset)는 자산의 위험도에 따라 다르게 산출된 자산의 가치입니다.
     * 일반론적으로 자산이 위험할수록 RWA가 높아지고, 은행은 그만큼 더 많은 자기자본을 보유해야 합니다.
     * 
     * IRB(내부등급법)는 은행이 자체적으로 개발한 등급 모델을 사용하여 리스크를 더 정밀하게 측정하는 방식입니다.
     * 1. 상관계수(R) 결정: 고객 유형별로 경제 상황에 따라 얼마나 같이 망할 가능성이 있는지 계산합니다.
     * 2. 자본부과율(K) 산출: 99.9% 신뢰구간에서 발생할 수 있는 최대 손실을 계산합니다.
     * 3. RWA = K * 12.5 * EAD (12.5는 자본비율 8% 기준의 역수입니다.)
     */
    public IrbResult calculateRwaIrb(BigDecimal pd, BigDecimal lgd, BigDecimal ead,
                                     double maturityYears,
                                     IrbRegulatoryParams params,
                                     CustomerType customerType,
                                     BigDecimal annualSales,
                                     String financialSectorCode) {
        
        if (pd == null || lgd == null || ead == null) return IrbResult.builder().rwa(BigDecimal.ZERO).build();
        if (params == null) params = IrbRegulatoryParams.builder().build();

        double pdVal = Math.max(pd.doubleValue(), params.getPdFloor().doubleValue());
        double lgdVal = lgd.doubleValue();

        // 1. 상관계수(R) 산출
        double r = calculateAssetCorrelation(pdVal, customerType, params, annualSales, financialSectorCode);

        // 2. 만기조정(b)
        double maturityAdj = 1.0;
        if (customerType != CustomerType.RETAIL) {
            double c1 = params.getMaturityConst1().doubleValue();
            double c2 = params.getMaturityConst2().doubleValue();
            double b = Math.pow(c1 - c2 * Math.log(pdVal), 2);
            maturityAdj = (1 + (maturityYears - 2.5) * b) / (1 - 1.5 * b);
        }

        // 3. 자본부과율(K) 산출
        NormalDistribution nd = new NormalDistribution();
        double gPd = nd.inverseCumulativeProbability(pdVal);
        double g999 = nd.inverseCumulativeProbability(0.999);
        double conditionalPD = nd.cumulativeProbability((gPd / Math.sqrt(1 - r)) + (Math.sqrt(r / (1 - r)) * g999));
        
        double k = (lgdVal * conditionalPD - pdVal * lgdVal) * maturityAdj;
        k = Math.max(k, 0);

        // 4. 최종 RWA
        BigDecimal rwa = BigDecimal.valueOf(k).multiply(MULTIPLIER_12_5, MC).multiply(ead, MC).setScale(4, RoundingMode.HALF_UP);

        return IrbResult.builder()
                .rwa(rwa)
                .kValue(BigDecimal.valueOf(k).setScale(8, RoundingMode.HALF_UP))
                .rValue(BigDecimal.valueOf(r).setScale(8, RoundingMode.HALF_UP))
                .maturityAdj(BigDecimal.valueOf(maturityAdj).setScale(8, RoundingMode.HALF_UP))
                .build();
    }

    private double calculateAssetCorrelation(double pdVal, CustomerType customerType,
                                             IrbRegulatoryParams params,
                                             BigDecimal annualSales,
                                             String finSectorCd) {
        String cacheKey = String.format("%f_%s_%s_%s", pdVal, customerType, annualSales, finSectorCd);
        if (correlationCache.containsKey(cacheKey)) return correlationCache.get(cacheKey);

        double expFactor = (1 - Math.exp(-50 * pdVal)) / (1 - Math.exp(-50));
        double r;

        switch (customerType) {
            case CORPORATE -> r = params.getAssetCorrBase().doubleValue() * expFactor + params.getAssetCorrHigh().doubleValue() * (1 - expFactor);
            case RETAIL -> r = params.getRetailCorrBase().doubleValue() * expFactor + params.getRetailCorrHigh().doubleValue() * (1 - expFactor);
            case SME -> {
                double baseR = params.getAssetCorrBase().doubleValue() * expFactor + params.getAssetCorrHigh().doubleValue() * (1 - expFactor);
                double sizeMax = params.getSmeSizeThreshold().doubleValue();
                double sizeMin = params.getSmeSizeMin().doubleValue();
                double sales = (annualSales != null) ? annualSales.doubleValue() : (sizeMax + sizeMin) / 2.0;
                sales = Math.max(sizeMin, Math.min(sales, sizeMax));
                r = baseR - 0.04 * (1 - (sales - sizeMin) / (sizeMax - sizeMin));
            }
            case FINANCIAL_INSTITUTION -> {
                double baseR = params.getAssetCorrBase().doubleValue() * expFactor + params.getAssetCorrHigh().doubleValue() * (1 - fPd(pdVal));
                double multiplier = params.getFiCorrMultiplier().doubleValue();
                if ("INSURANCE".equalsIgnoreCase(finSectorCd)) multiplier = 1.1;
                r = Math.min(baseR * multiplier, params.getFiCorrCap().doubleValue());
            }
            default -> r = 0.15;
        }
        correlationCache.put(cacheKey, r);
        return r;
    }

    private double fPd(double pdVal) { return (1 - Math.exp(-50 * pdVal)) / (1 - Math.exp(-50)); }

    public BigDecimal calculateRwaSa(BigDecimal ead, BigDecimal standardRw) {
        if (ead == null || standardRw == null) return BigDecimal.ZERO;
        return ead.multiply(standardRw, MC).setScale(4, RoundingMode.HALF_UP);
    }

    @Getter
    @Builder
    public static class IrbResult {
        private final BigDecimal rwa;
        private final BigDecimal kValue;
        private final BigDecimal rValue;
        private final BigDecimal maturityAdj;
    }

    @Deprecated(since = "2.0")
    public IrbResult calculateRwaIrb(BigDecimal pd, BigDecimal lgd, BigDecimal ead, double maturityYears) {
        return calculateRwaIrb(pd, lgd, ead, maturityYears, null, CustomerType.CORPORATE, null, null);
    }
}
