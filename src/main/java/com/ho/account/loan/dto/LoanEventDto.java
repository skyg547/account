package com.ho.account.loan.dto;

import com.ho.account.loan.domain.LoanEvent;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대출 이벤트 (LoanEvent) 응답 DTO
 */
@Data
@Builder
public class LoanEventDto {
    private Long id;
    private Long loanId;
    private String loanNumber;
    private LoanEvent.EventType eventType;
    private LocalDate eventDate;
    private String description;
    private Long relatedJournalEntryId;
    private String relatedJournalEntrySlipNo;
    private Long recalculationRunId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static LoanEventDto fromEntity(LoanEvent entity) {
        return LoanEventDto.builder()
                .id(entity.getId())
                .loanId(entity.getLoan() != null ? entity.getLoan().getId() : null)
                .loanNumber(entity.getLoan() != null ? entity.getLoan().getLoanNumber() : null)
                .eventType(entity.getEventType())
                .eventDate(entity.getEventDate())
                .description(entity.getDescription())
                .relatedJournalEntryId(entity.getRelatedJournalEntry() != null ? entity.getRelatedJournalEntry().getId() : null)
                .relatedJournalEntrySlipNo(entity.getRelatedJournalEntry() != null ? entity.getRelatedJournalEntry().getSlipNo() : null)
                .recalculationRunId(entity.getRecalculationRun() != null ? entity.getRecalculationRun().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
