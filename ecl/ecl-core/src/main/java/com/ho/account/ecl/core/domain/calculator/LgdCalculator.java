package com.ho.account.ecl.core.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * [도메인 계산기] IFRS 9 LGD(Loss Given Default, 부도시 손실률) 정밀 산출 도메인 모델.
 *
 * 💡 [초보자를 위한 개념 설명]
 * LGD는 부도가 발생했을 때 은행이 실제로 회수하지 못하고 잃게 되는 익스포저 비율입니다.
 * - 무담보부 익스포저 LGD (Unsecured LGD): 담보가 없는 대출의 예상 손실 비율 (예: 45%)
 * - 담보부 익스포저 LGD (Secured LGD): 예금/부동산 담보가 가용할 때의 예상 손실 비율 (예: 10%)
 * - Downturn LGD Adjustment (침체기 LGD 보정): 경기 침체기 시나리오 시 회수 가치 하락을 보수적으로 반영하는 계수입니다.
 */
@Component
public class LgdCalculator {

    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);

    /**
     * 담보 가치 및 무담보 비중을 감안한 가중 LGD를 계산하고, Downturn LGD 하한선 정책을 반영합니다.
     *
     * @param securedAmt      담보로 보증/보호되는 EAD 금액
     * @param unsecuredAmt    무보증 EAD 금액
     * @param totalEad        전체 EAD (securedAmt + unsecuredAmt)
     * @param securedLgd      담보부 세그먼트 LGD (예: 0.10)
     * @param unsecuredLgd    무담보부 세그먼트 LGD (예: 0.45)
     * @param downturnFactor  경기 침체기 보정 계수 (1.0 = 표준, 1.2 = 침체기 20% 할증)
     * @param lgdFloor        LGD 하한선 (예: 0.05)
     * @return 최종 가중 LGD 비율 (소수점 6자리)
     */
    public BigDecimal calculateWeightedLgd(
            BigDecimal securedAmt,
            BigDecimal unsecuredAmt,
            BigDecimal totalEad,
            BigDecimal securedLgd,
            BigDecimal unsecuredLgd,
            BigDecimal downturnFactor,
            BigDecimal lgdFloor) {

        BigDecimal safeSecuredAmt = (securedAmt != null) ? securedAmt : BigDecimal.ZERO;
        BigDecimal safeUnsecuredAmt = (unsecuredAmt != null) ? unsecuredAmt : BigDecimal.ZERO;
        BigDecimal safeTotalEad = (totalEad != null) ? totalEad : safeSecuredAmt.add(safeUnsecuredAmt);

        BigDecimal safeSecuredLgd = (securedLgd != null) ? securedLgd : new BigDecimal("0.10");
        BigDecimal safeUnsecuredLgd = (unsecuredLgd != null) ? unsecuredLgd : new BigDecimal("0.45");
        BigDecimal safeDownturn = (downturnFactor != null) ? downturnFactor : BigDecimal.ONE;
        BigDecimal safeFloor = (lgdFloor != null) ? lgdFloor : new BigDecimal("0.05");

        if (safeTotalEad.compareTo(BigDecimal.ZERO) <= 0) {
            return safeUnsecuredLgd.setScale(6, RoundingMode.HALF_UP);
        }

        // 가중 LGD = (SecuredAmt * SecuredLGD + UnsecuredAmt * UnsecuredLGD) / TotalEAD
        BigDecimal securedLoss = safeSecuredAmt.multiply(safeSecuredLgd, MC);
        BigDecimal unsecuredLoss = safeUnsecuredAmt.multiply(safeUnsecuredLgd, MC);
        BigDecimal totalLoss = securedLoss.add(unsecuredLoss, MC);

        BigDecimal rawWeightedLgd = totalLoss.divide(safeTotalEad, MC);

        // 경기 침체기 LGD 할증 (Downturn LGD)
        BigDecimal downturnLgd = rawWeightedLgd.multiply(safeDownturn, MC);

        // LGD Floor 반영 및 최대 1.0 캡 적용
        BigDecimal finalLgd = downturnLgd.max(safeFloor);
        if (finalLgd.compareTo(BigDecimal.ONE) > 0) {
            finalLgd = BigDecimal.ONE;
        }

        return finalLgd.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * [도메인 계산기] 담보 유무 및 모델 파라미터 규제에 따른 LGD Floor(하한선)를 반영합니다.
     *
     * 💡 [IFRS 9 모델 가이드 & DDD 설계]
     * 담보가 있는 자산(Secured)은 예상치 못한 담보 가치 하락에 대비해 최소 손실률(Secured Floor, 약 10%)을 적용하고,
     * 무담보 자산(Unsecured)은 훨씬 더 높은 최저선(Unsecured Floor, 약 25~45%)을 적용하여 보수적으로 손실률을 결정합니다.
     *
     * @param baseLgd 세그먼트 마스터 LGD 수치
     * @param hasCollateral 담보 유무
     * @param securedFloor 담보부 LGD 하한선 (예: 0.10)
     * @param unsecuredFloor 무담보부 LGD 하한선 (예: 0.45)
     * @return Floor 규제가 반영된 최종 LGD 수치
     */
    public BigDecimal applyLgdFloor(BigDecimal baseLgd, boolean hasCollateral, BigDecimal securedFloor, BigDecimal unsecuredFloor) {
        BigDecimal safeLgd = (baseLgd != null) ? baseLgd : ((unsecuredFloor != null) ? unsecuredFloor : new BigDecimal("0.45"));
        BigDecimal safeSecuredFloor = (securedFloor != null) ? securedFloor : new BigDecimal("0.10");
        BigDecimal safeUnsecuredFloor = (unsecuredFloor != null) ? unsecuredFloor : new BigDecimal("0.45");

        BigDecimal finalLgd = hasCollateral
                ? safeLgd.max(safeSecuredFloor)
                : safeLgd.max(safeUnsecuredFloor);

        if (finalLgd.compareTo(BigDecimal.ONE) > 0) {
            finalLgd = BigDecimal.ONE;
        }

        return finalLgd.setScale(6, RoundingMode.HALF_UP);
    }
}

