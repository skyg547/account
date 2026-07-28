package com.ho.account.contracts.masterdata;

import java.util.Locale;

/**
 * 계정과목 모듈 간 조회 결과입니다.
 *
 * <p>정상잔액 방향은 외화평가와 잔액 증감의 차대 판단에 사용되므로 알 수 없는 값을 조용히
 * 기본값으로 바꾸지 않습니다.</p>
 */
public record AccountSubjectRef(
        String code,
        String name,
        boolean unsettled,
        boolean fixedAsset,
        String normalBalanceSide) {

    private static final String DEBIT = "DEBIT";
    private static final String CREDIT = "CREDIT";

    public AccountSubjectRef {
        normalBalanceSide = normalizeNormalBalanceSide(normalBalanceSide);
    }

    /**
     * 기존 호출부 호환 생성자입니다.
     *
     * <p>@todo 모든 제공자가 정상잔액 방향을 명시한 뒤 이 암묵적 DEBIT 생성자를 제거한다.
     * 부채/자본/수익 계정에 기본 DEBIT가 적용되면 결산 손익 방향이 반대로 계산될 수 있다.</p>
     */
    public AccountSubjectRef(String code, String name, boolean unsettled, boolean fixedAsset) {
        this(code, name, unsettled, fixedAsset, DEBIT);
    }

    public boolean debitNormalBalance() {
        return DEBIT.equals(normalBalanceSide);
    }

    public boolean creditNormalBalance() {
        return CREDIT.equals(normalBalanceSide);
    }

    private static String normalizeNormalBalanceSide(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("normalBalanceSide must not be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!DEBIT.equals(normalized) && !CREDIT.equals(normalized)) {
            throw new IllegalArgumentException(
                    "normalBalanceSide must be DEBIT or CREDIT: " + normalized);
        }
        return normalized;
    }
}