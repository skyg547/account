package com.ho.account.receivable.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 수동 수납 매칭 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 자동 매칭이 실패했거나 사람이 직접 판단해야 할 때, 어떤 수납과 어떤 채권을
 * 얼마만큼 연결할지 한 번에 전달합니다. 두 잔액을 함께 줄여야 하므로 세 값이 모두 필요합니다.</p>
 */
public record ManualMatchingCommand(Long collectionId, Long receivableId, BigDecimal matchingAmount) {
    public ManualMatchingCommand {
        if (collectionId == null || collectionId <= 0) {
            throw new IllegalArgumentException("collectionId must be greater than zero");
        }
        if (receivableId == null || receivableId <= 0) {
            throw new IllegalArgumentException("receivableId must be greater than zero");
        }
        matchingAmount = Objects.requireNonNull(matchingAmount, "matchingAmount is required");
        if (matchingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("matchingAmount must be greater than zero");
        }
    }
}