package com.ho.account.loan.dto;

import com.ho.account.loan.domain.DeferredItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 이연 항목 (DeferredItem) 응답 DTO
 */
@Data
@Builder
public class DeferredItemDto {
    private Long id;
    private Long loanId;
    private String loanNumber;
    private Long deferredItemTypeId;
    private String deferredItemTypeCode;
    private String deferredItemTypeName;
    private BigDecimal amount;
    private LocalDate deferralDate;
    private LocalDate amortizationStartDate;
    private LocalDate amortizationEndDate;
    private BigDecimal remainingAmount;
    private Long initialJournalEntryId;
    private String initialJournalEntrySlipNo;
    private DeferredItem.DeferredItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static DeferredItemDto fromEntity(DeferredItem entity) {
        return DeferredItemDto.builder()
                .id(entity.getId())
                .loanId(entity.getLoan() != null ? entity.getLoan().getId() : null)
                .loanNumber(entity.getLoan() != null ? entity.getLoan().getLoanNumber() : null)
                .deferredItemTypeId(entity.getDeferredItemType() != null ? entity.getDeferredItemType().getId() : null)
                .deferredItemTypeCode(entity.getDeferredItemType() != null ? entity.getDeferredItemType().getCode() : null)
                .deferredItemTypeName(entity.getDeferredItemType() != null ? entity.getDeferredItemType().getName() : null)
                .amount(entity.getAmount())
                .deferralDate(entity.getDeferralDate())
                .amortizationStartDate(entity.getAmortizationStartDate())
                .amortizationEndDate(entity.getAmortizationEndDate())
                .remainingAmount(entity.getRemainingAmount())
                .initialJournalEntryId(entity.getInitialJournalEntryId())
                .initialJournalEntrySlipNo(entity.getInitialJournalEntrySlipNo())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
