package com.ho.account.contracts.expenditure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        String detailDescription,
        List<LeasePaymentResolutionLineCommand> debitLines
) {
    public LeasePaymentResolutionCommand {
        if (debitLines == null || debitLines.isEmpty()) {
            debitLines = List.of(new LeasePaymentResolutionLineCommand(
                    debitAccountCode,
                    amount,
                    detailDescription));
        } else {
            debitLines = List.copyOf(debitLines);
        }

        BigDecimal totalAmount = debitLines.stream()
                .map(LeasePaymentResolutionLineCommand::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (amount == null) {
            amount = totalAmount;
        } else if (amount.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("amount must equal the sum of debit line amounts.");
        }
        if (debitAccountCode == null && debitLines.size() == 1) {
            debitAccountCode = debitLines.get(0).debitAccountCode();
        }
        if (detailDescription == null && debitLines.size() == 1) {
            detailDescription = debitLines.get(0).detailDescription();
        }
    }

    public LeasePaymentResolutionCommand(
            String title,
            LocalDate resolutionDate,
            LocalDate paymentDate,
            String departmentCode,
            String debitAccountCode,
            String creditAccountCode,
            String businessPartnerCode,
            BigDecimal amount,
            String detailDescription) {
        this(
                title,
                resolutionDate,
                paymentDate,
                departmentCode,
                debitAccountCode,
                creditAccountCode,
                businessPartnerCode,
                amount,
                detailDescription,
                null);
    }

    public LeasePaymentResolutionCommand(
            String title,
            LocalDate resolutionDate,
            LocalDate paymentDate,
            String departmentCode,
            String creditAccountCode,
            String businessPartnerCode,
            List<LeasePaymentResolutionLineCommand> debitLines) {
        this(
                title,
                resolutionDate,
                paymentDate,
                departmentCode,
                null,
                creditAccountCode,
                businessPartnerCode,
                null,
                null,
                debitLines);
    }
}
