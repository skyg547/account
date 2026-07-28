package com.ho.account.loan.dto;

// HTTP 응답 계약은 loan:api 인바운드 어댑터가 소유합니다.

import com.ho.account.loan.domain.LoanDisbursal;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대출 실행 (LoanDisbursal) 응답 DTO
 */
@Data
@Builder
public class LoanDisbursalDto {
    private Long id;
    private Long loanId;
    private String loanNumber;
    private LocalDate disbursalDate;
    private BigDecimal disbursedAmount;
    private Long journalEntryId;
    private String journalEntrySlipNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static LoanDisbursalDto fromEntity(LoanDisbursal entity) {
        return LoanDisbursalDto.builder()
                .id(entity.getId())
                .loanId(entity.getLoan() != null ? entity.getLoan().getId() : null)
                .loanNumber(entity.getLoan() != null ? entity.getLoan().getLoanNumber() : null)
                .disbursalDate(entity.getDisbursalDate())
                .disbursedAmount(entity.getDisbursedAmount())
                .journalEntryId(entity.getJournalEntryId())
                .journalEntrySlipNo(entity.getJournalEntrySlipNo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
