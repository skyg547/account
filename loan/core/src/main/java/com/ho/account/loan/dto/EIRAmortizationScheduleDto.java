package com.ho.account.loan.dto;

import com.ho.account.loan.domain.EIRAmortizationSchedule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * EIR 상각 스케줄(EIRAmortizationSchedule) 응답 DTO.
 *
 * <p>화면/API에는 전표 엔티티 전체가 아니라 추적 가능한 전표 ID와 전표번호만 노출합니다.
 */
@Data
@Builder
public class EIRAmortizationScheduleDto {
    private Long id;
    private Long loanId;
    private String loanNumber;
    private LocalDate scheduleDate;
    private BigDecimal beginningBalance;
    private BigDecimal interestIncome;
    private BigDecimal principalRepayment;
    private BigDecimal endingBalance;
    private BigDecimal deferredItemAmortization;
    private BigDecimal cashFlow;
    private Long amortizationJournalEntryId;
    private String amortizationJournalEntrySlipNo;
    private boolean isRecalculated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static EIRAmortizationScheduleDto fromEntity(EIRAmortizationSchedule entity) {
        return EIRAmortizationScheduleDto.builder()
                .id(entity.getId())
                .loanId(entity.getLoan() != null ? entity.getLoan().getId() : null)
                .loanNumber(entity.getLoan() != null ? entity.getLoan().getLoanNumber() : null)
                .scheduleDate(entity.getScheduleDate())
                .beginningBalance(entity.getBeginningBalance())
                .interestIncome(entity.getInterestIncome())
                .principalRepayment(entity.getPrincipalRepayment())
                .endingBalance(entity.getEndingBalance())
                .deferredItemAmortization(entity.getDeferredItemAmortization())
                .cashFlow(entity.getCashFlow())
                .amortizationJournalEntryId(entity.getAmortizationJournalEntryId())
                .amortizationJournalEntrySlipNo(entity.getAmortizationJournalEntrySlipNo())
                .isRecalculated(entity.isRecalculated())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}