package com.ho.account.ecl.core.domain.calculator;

import java.math.BigDecimal;

/**
 * EAD/CRM 산출 결과 값 객체.
 *
 * <p>배열 인덱스 대신 업무 이름이 있는 필드로 결과를 전달합니다. 호출자는
 * {@code result.eadStar()}처럼 값의 의미를 직접 읽을 수 있어, 산출 순서가 바뀌어도
 * 잘못된 금액을 다른 필드에 저장하는 실수를 방지할 수 있습니다.</p>
 *
 * @param eadStar 담보 공제 후 최종 위험 노출액
 * @param appliedCcf 미사용 한도에 적용한 신용전환계수
 * @param eadRaw 담보 공제 전 원시 EAD
 * @param crmDeduction 담보로 공제한 금액
 * @param weightedLgd 담보부/무담보부 비중을 반영한 가중 LGD
 */
public record EadCalculationResult(
        BigDecimal eadStar,
        BigDecimal appliedCcf,
        BigDecimal eadRaw,
        BigDecimal crmDeduction,
        BigDecimal weightedLgd) {
}
