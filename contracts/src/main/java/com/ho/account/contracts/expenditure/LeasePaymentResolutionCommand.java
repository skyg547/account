package com.ho.account.contracts.expenditure;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 리스료 지급 결의 요청 명령.
 * 리스료 지급 시 전표 생성을 위한 정보를 포함합니다.
 */
public record LeasePaymentResolutionCommand(
        String title,
        LocalDate resolutionDate,
        LocalDate paymentDate,
        String departmentCode,
        String debitAccountCode,   // 차변 계정 (예: 리스부채 또는 리스비용)
        String creditAccountCode,  // 대변 계정 (예: 미지급금 또는 현금/예금)
        String businessPartnerCode,
        BigDecimal amount,
        String detailDescription
) {
}
