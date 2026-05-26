package com.risk.credit.core.domain.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [모델러] 부도 시 익스포저(EAD) 및 신용위험완화(CRM) 산출 엔진.
 *
 * 이 클래스는 고객이 부도가 났을 때 은행이 최종적으로 노출되어 있는 '위험 금액'을 추정합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 1. EAD (Exposure at Default): 고객이 부도가 나는 바로 그 순간, 은행이 떼일 수 있는 총 금액입니다.
 * 2. CCF (신용전환계수): 아직 빌려주지 않은 약정 한도(예: 마이너스 통장 미사용 한도) 중
 *    부도 직전에 고객이 실제로 뽑아 쓸 것으로 예상되는 비율입니다.
 * 3. CRM (신용위험완화): 담보(예: 예금 담보)를 잡고 있다면 떼일 돈이 줄어들겠죠?
 *    이처럼 리스크를 줄이는 기법을 의미합니다.
 * 4. Haircut (헤어컷): 담보의 가치가 나중에 떨어질 것을 대비해,
 *    미래 가치에서 일정 비율을 깎고 인정해주는 것입니다.
 * 5. EAD* (보정 EAD): CRM(담보)을 반영하여 최종 보정된 위험 금액입니다.
 *
 * 🔧 [v2.0 고도화 내역]
 * - 기존: 담보부 LGD(0.20), 무담보부 LGD(0.40) 하드코딩
 * - 변경: {@link IrbRegulatoryParams}에서 로드한 securedLgdFloor/unsecuredLgdFloor 사용
 */
@Component
public class EadCalculator {

    /**
     * [고도화] 바젤 기반 고도화된 EAD* (보정 EAD) 산출.
     *
     * 💡 [산출 흐름 요약]
     * ① 미사용 잔액 = 약정 한도 - 현재 잔액
     * ② 예상 EAD = 현재 잔액 + (미사용 잔액 × CCF)
     * ③ CRM 공제액 = 담보 가치 × (1 - 헤어컷)
     * ④ 가중 LGD = (담보부 금액 × 담보부 LGD + 무담보부 금액 × 무담보부 LGD) / EAD
     * ⑤ EAD* = EAD - CRM 공제액
     *
     * @param outstandingAmt   현재 실행 잔액 (On-Balance)
     * @param notionalAmt      약정 한도 금액 (Limit)
     * @param ccfRate          상품별 신용전환계수 (CCF)
     * @param collateralAmt    배분된 담보 가치 합산액
     * @param totalHaircut     가중평균 헤어컷 (Hc + Hfx)
     * @param securedLgd       담보부(Secured) 익스포저 LGD (규제 파라미터에서 로드)
     * @param unsecuredLgd     무담보부(Unsecured) 익스포저 LGD (규제 파라미터에서 로드)
     * @return [보정 EAD*, 적용 CCF, 원시 EAD, CRM 공제액, 가중 LGD]
     */
    public Object[] calculateAdvancedEAD(BigDecimal outstandingAmt,
                                         BigDecimal notionalAmt,
                                         BigDecimal ccfRate,
                                         BigDecimal collateralAmt,
                                         BigDecimal totalHaircut,
                                         BigDecimal securedLgd,
                                         BigDecimal unsecuredLgd) {

        // ── null 방어 처리 ──
        if (outstandingAmt == null) outstandingAmt = BigDecimal.ZERO;
        if (notionalAmt == null) notionalAmt = outstandingAmt;
        if (ccfRate == null) ccfRate = BigDecimal.ZERO;
        if (collateralAmt == null) collateralAmt = BigDecimal.ZERO;
        if (totalHaircut == null) totalHaircut = BigDecimal.ZERO;
        if (securedLgd == null) securedLgd = new BigDecimal("0.20");
        if (unsecuredLgd == null) unsecuredLgd = new BigDecimal("0.45");

        // ── 1단계: 미사용 잔액 계산 ──
        // 미사용 잔액 = 약정 한도 - 현재 잔액 (음수가 되면 0 처리)
        BigDecimal undrawnAmt = notionalAmt.subtract(outstandingAmt);
        if (undrawnAmt.compareTo(BigDecimal.ZERO) < 0) {
            undrawnAmt = BigDecimal.ZERO;
        }

        // ── 2단계: 예상 EAD 계산 ──
        // 💡 미사용 한도에 CCF를 곱해 "부도 직전에 추가로 쓸 금액"을 예측합니다.
        // EAD = 현재 잔액(On-Balance) + 미사용 한도 × CCF(Off-Balance)
        BigDecimal offBalanceEad = undrawnAmt.multiply(ccfRate);
        BigDecimal ead = outstandingAmt.add(offBalanceEad).setScale(4, RoundingMode.HALF_UP);

        // ── 3단계: CRM 공제액 산출 ──
        // 💡 담보의 물리적 가치에서 헤어컷(미래 가치 하락 예상분)을 차감합니다.
        // CRM 공제액 = 담보가치 × (1 - 헤어컷)
        BigDecimal crmDeduction = collateralAmt
                .multiply(BigDecimal.ONE.subtract(totalHaircut))
                .setScale(4, RoundingMode.HALF_UP);
        // CRM 공제액은 EAD를 초과할 수 없음 (마이너스 방지)
        if (crmDeduction.compareTo(ead) > 0) {
            crmDeduction = ead;
        }

        // ── 4단계: 가중 LGD 산출 ──
        // 💡 담보가 커버하는 부분(Secured)과 커버하지 못하는 부분(Unsecured)에
        //    각각 다른 LGD를 적용한 후, EAD 기준으로 가중 평균합니다.
        BigDecimal securedAmt = crmDeduction;       // 담보로 보호되는 금액
        BigDecimal unsecuredAmt = ead.subtract(securedAmt);  // 무보증 노출 금액

        BigDecimal weightedLgd = BigDecimal.ZERO;
        if (ead.compareTo(BigDecimal.ZERO) > 0) {
            // 가중 LGD = (담보부 금액 × 담보부 LGD + 무담보부 금액 × 무담보부 LGD) / 전체 EAD
            weightedLgd = (securedAmt.multiply(securedLgd).add(unsecuredAmt.multiply(unsecuredLgd)))
                    .divide(ead, 6, RoundingMode.HALF_UP);
        }

        // ── 5단계: 최종 보정 EAD* 산출 ──
        // EAD* = EAD - CRM 공제액
        // 💡 이것이 규제 자본 산출의 최종 입력값이 됩니다.
        BigDecimal eadStar = ead.subtract(crmDeduction).setScale(4, RoundingMode.HALF_UP);

        return new Object[]{eadStar, ccfRate, ead, crmDeduction, weightedLgd};
    }

    /**
     * [레거시 호환] 기존 시그니처 유지.
     * 새 코드에서는 반드시 7-파라미터 버전을 사용하세요.
     *
     * @deprecated v2.0부터 securedLgd/unsecuredLgd 파라미터를 받는 오버로드 메서드를 사용하세요.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public Object[] calculateAdvancedEAD(BigDecimal outstandingAmt,
                                         BigDecimal notionalAmt,
                                         BigDecimal ccfRate,
                                         BigDecimal collateralAmt,
                                         BigDecimal totalHaircut) {
        // 기존 하드코딩 LGD 값으로 신규 메서드에 위임
        return calculateAdvancedEAD(outstandingAmt, notionalAmt, ccfRate,
                collateralAmt, totalHaircut,
                new BigDecimal("0.20"),   // securedLgd (기존 하드코딩 값)
                new BigDecimal("0.45")); // unsecuredLgd (기존 하드코딩 값 보정: 0.40→0.45 규제 기준)
    }
}
