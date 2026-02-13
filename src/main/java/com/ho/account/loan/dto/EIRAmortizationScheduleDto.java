package com.ho.account.loan.dto;

import com.ho.account.loan.domain.EIRAmortizationSchedule;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EIR 상각 스케줄 (EIRAmortizationSchedule) 응답 DTO
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
                .amortizationJournalEntryId(entity.getAmortizationJournalEntry() != null ? entity.getAmortizationJournalEntry().getId() : null)
                .amortizationJournalEntrySlipNo(entity.getAmortizationJournalEntry() != null ? entity.getAmortizationJournalEntry().getSlipNo() : null)
                .isRecalculated(entity.isRecalculated())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
