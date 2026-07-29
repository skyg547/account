package com.ho.account.closing.dto;

import com.ho.account.closing.domain.DailyClosingStatus;
import com.ho.account.closing.domain.EodState;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일마감 상태 조회 응답입니다.
 *
 * <p>도메인 엔티티를 그대로 노출하지 않고, 운영자가 상태 전이의 처리자와 시각을 함께
 * 감사할 수 있는 읽기 전용 API 계약으로 변환합니다.</p>
 */
public record EodStatusDto(
        LocalDate businessDate,
        EodState state,
        boolean transactionAllowed,
        Long version,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime preparedAt,
        String preparedBy,
        LocalDateTime closingStartedAt,
        String closingStartedBy,
        LocalDateTime closedAt,
        String closedBy,
        LocalDateTime bodStartedAt,
        String bodStartedBy,
        LocalDateTime openedAt,
        String openedBy) {

    public static EodStatusDto from(DailyClosingStatus status) {
        return new EodStatusDto(
                status.getBusinessDate(),
                status.getState(),
                status.getState().isTransactionAllowed(),
                status.getVersion(),
                status.getCreatedAt(),
                status.getCreatedBy(),
                status.getUpdatedAt(),
                status.getUpdatedBy(),
                status.getPreparedAt(),
                status.getPreparedBy(),
                status.getClosingStartedAt(),
                status.getClosingStartedBy(),
                status.getClosedAt(),
                status.getClosedBy(),
                status.getBodStartedAt(),
                status.getBodStartedBy(),
                status.getOpenedAt(),
                status.getOpenedBy());
    }
}
