package com.ho.account.closing.dto;

import java.time.LocalDate;

/**
 * 조회 시점의 일반 전표 허용 여부입니다.
 *
 * <p>이 날짜별 응답은 전기 커밋 허가나 결산 조정 전표의 우회 권한을 부여하지 않습니다.</p>
 */
public record ClosingAdmissionStatusDto(
        LocalDate accountingDate,
        boolean ordinaryPostingAllowed) {
}
