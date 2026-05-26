package com.risk.credit.core.domain.calculator;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * [Value Object] 바젤 IRB(내부등급법) 규제 파라미터 집합.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 바젤 IRB 산식에는 여러 '상수가 들어갑니다. 예를 들어:
 *   - "PD(부도율)는 아무리 우량 고객이라도 최소 0.05%는 되어야 한다" → PD Floor
 *   - "자산상관계수(R)는 0.12에서 0.24 사이에서 결정된다" → Asset Correlation 가중치
 *   - "만기조정 계수는 특정 상수로 계산한다" → Maturity Adjustment 상수
 *
 * 이런 값들을 코드에 직접 쓰면(하드코딩), 규제가 바뀔 때마다 코드를 수정해야 합니다.
 * 이 VO를 통해 DB에서 값을 읽어오므로, 규제 변경 시 DB만 업데이트하면 됩니다.
 *
 * ⚠️ 이 객체는 불변(Immutable)입니다. 생성 후 값을 변경할 수 없습니다.
 */
@Getter
@Builder
public class IrbRegulatoryParams {

    // ==========================================
    // 1. PD(부도율) 관련 규제 파라미터
    // ==========================================

    /**
     * PD 하한선 (PD Floor).
     * 바젤 규제상 PD는 이 값 밑으로 내려갈 수 없습니다.
     * 예: 0.0005 (= 0.05%)
     *
     * 💡 아무리 신용이 좋은 고객이라도 부도 확률이 0%는 아니라는 규제 철학을 반영합니다.
     */
    @Builder.Default
    private final BigDecimal pdFloor = new BigDecimal("0.0005");

    // ==========================================
    // 2. 자산상관계수(Asset Correlation, R) 관련
    // ==========================================

    /**
     * 자산상관계수 하한 가중치 (기업 익스포저 기준).
     * 바젤 산식에서 R = corrBase × f(PD) + corrHigh × (1 - f(PD)) 형태로 사용됩니다.
     * 기본값: 0.12 (= 12%)
     *
     * 💡 PD가 높을수록(위험할수록) 이 작은 값에 가까워져, 상관계수가 낮아집니다.
     *    직관적으로 "이미 위험한 기업은 시장 전체 움직임보다 자체 리스크가 더 크다"는 뜻입니다.
     */
    @Builder.Default
    private final BigDecimal assetCorrBase = new BigDecimal("0.12");

    /**
     * 자산상관계수 상한 가중치 (기업 익스포저 기준).
     * 기본값: 0.24 (= 24%)
     *
     * 💡 PD가 낮을수록(우량할수록) 이 큰 값에 가까워져, 상관계수가 높아집니다.
     *    "우량 기업은 개별 리스크보다 경기 전체(시스템적 리스크)에 더 영향받는다"는 뜻입니다.
     */
    @Builder.Default
    private final BigDecimal assetCorrHigh = new BigDecimal("0.24");

    // ==========================================
    // 3. 소매(Retail) 전용 상관계수
    // ==========================================

    /**
     * 소매 익스포저 상관계수 하한.
     * 기본값: 0.03 (= 3%)
     *
     * 💡 개인 고객은 기업보다 시장 전체 움직임의 영향을 덜 받기 때문에 상관계수가 작습니다.
     */
    @Builder.Default
    private final BigDecimal retailCorrBase = new BigDecimal("0.03");

    /**
     * 소매 익스포저 상관계수 상한.
     * 기본값: 0.16 (= 16%)
     */
    @Builder.Default
    private final BigDecimal retailCorrHigh = new BigDecimal("0.16");

    // ==========================================
    // 4. 만기조정(Maturity Adjustment) 관련
    // ==========================================

    /**
     * 만기조정 산식 상수 1.
     * b(PD) = (maturityConst1 - maturityConst2 × ln(PD))² 에서 사용됩니다.
     * 기본값: 0.11852
     */
    @Builder.Default
    private final BigDecimal maturityConst1 = new BigDecimal("0.11852");

    /**
     * 만기조정 산식 상수 2.
     * 기본값: 0.05478
     */
    @Builder.Default
    private final BigDecimal maturityConst2 = new BigDecimal("0.05478");

    // ==========================================
    // 5. SME 보정 관련
    // ==========================================

    /**
     * SME 매출액 상관계수 보정 상한 (단위: 백만 EUR 또는 억 원).
     * 바젤 SME 보정: R_adj = R × (1 - (1 - (S-5)/45))
     * 기본값: 50 (= 매출 50억 이상이면 보정 미적용)
     *
     * 💡 중소기업(SME)은 대기업보다 시스템적 리스크에 덜 노출되므로,
     *    매출 규모에 따라 상관계수를 깎아주는 혜택을 줍니다.
     */
    @Builder.Default
    private final BigDecimal smeSizeThreshold = new BigDecimal("50");

    // ==========================================
    // 6. LGD(부도시손실률) 관련 규제 파라미터
    // ==========================================

    /**
     * 담보부(Secured) 익스포저의 규제 LGD 하한.
     * FIRB(기초내부등급법)에서 담보가 있으면 적용하는 최소 LGD.
     * 기본값: 0.20 (= 20%)
     *
     * 💡 담보가 있어도 최소 20%의 손실은 발생할 수 있다는 규제적 보수성을 반영합니다.
     */
    @Builder.Default
    private final BigDecimal securedLgdFloor = new BigDecimal("0.20");

    /**
     * 무담보(Unsecured) 익스포저의 규제 LGD 기준값.
     * FIRB에서 담보가 없으면 적용하는 기본 LGD.
     * 기본값: 0.45 (= 45%)
     */
    @Builder.Default
    private final BigDecimal unsecuredLgdFloor = new BigDecimal("0.45");

    // ==========================================
    // 7. 금융기관 전용 보정
    // ==========================================

    /**
     * 금융기관(Financial Institution) 상관계수 배수.
     * 금융기관은 시스템적 위험이 스며드는 경향이 커서 상관계수를 상향 보정합니다.
     * 기본값: 1.25 (= 기업 상관계수의 125%)
     */
    @Builder.Default
    private final BigDecimal fiCorrMultiplier = new BigDecimal("1.25");

    /**
     * 금융기관 상관계수 상한 (Correlation Cap).
     * 보정된 상관계수는 이 값을 초과할 수 없습니다.
     * 기본값: 0.30 (= 30%)
     */
    @Builder.Default
    private final BigDecimal fiCorrCap = new BigDecimal("0.30");

    /**
     * SME 매출액 보정 최소 기준 (단위: 억 원).
     * 매출액이 이 값 미만이면 이 값을 사용하여 보정합니다.
     * 기본값: 5
     */
    @Builder.Default
    private final BigDecimal smeSizeMin = new BigDecimal("5");

    /**
     * ECL(기대손실) 산출 시 사용할 기본 할인율.
     * 기본값: 0.05 (= 5%)
     */
    @Builder.Default
    private final BigDecimal defaultDiscountRate = new BigDecimal("0.05");
}
