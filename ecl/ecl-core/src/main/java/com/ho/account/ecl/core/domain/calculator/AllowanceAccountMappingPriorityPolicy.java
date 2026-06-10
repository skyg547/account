package com.ho.account.ecl.core.domain.calculator;

/**
 * 대손충당금 계정 매핑 우선순위 정책.
 *
 * <p>대량 집계 SQL도 같은 순서로 {@code ROW_NUMBER()}를 계산합니다:
 * 업무단위 정확 일치가 공통(null)보다 우선하고, 그 다음 통화 정확 일치가 공통(null)보다 우선합니다.
 */
public final class AllowanceAccountMappingPriorityPolicy {

    private AllowanceAccountMappingPriorityPolicy() {
    }

    public static int score(String mappingBusinessUnit, String mappingCurrency, String businessUnit, String currency) {
        return matchScore(mappingBusinessUnit, businessUnit) * 10 + matchScore(mappingCurrency, currency);
    }

    private static int matchScore(String mappingValue, String actualValue) {
        if (mappingValue == null) return 1;
        return mappingValue.equals(actualValue) ? 2 : 0;
    }
}
