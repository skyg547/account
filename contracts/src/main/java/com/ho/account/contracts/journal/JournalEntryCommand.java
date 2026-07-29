package com.ho.account.contracts.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 업무 모듈이 journal-ledger에 임시 전표 생성을 요청할 때 사용하는 계약입니다.
 *
 * <p>호출 순서는 명령 생성 -> 계약 형식 검증 -> journal-ledger 도메인 검증 -> 저장입니다.
 * 이 객체는 목록을 불변 복사해 호출 이후 원본 List 변경이 전표 내용을 바꾸지 못하게 합니다.</p>
 */
public record JournalEntryCommand(
        LocalDate slipDate,
        LocalDate accountingDate,
        String description,
        String entryType,
        String currencyCode,
        BigDecimal exchangeRate,
        String createdBy,
        String auditUser,
        String lineageSourceType,
        String lineageSourceId,
        String slipNo,
        List<JournalLineCommand> lines) {

    public JournalEntryCommand {
        Objects.requireNonNull(slipDate, "slipDate must not be null");
        Objects.requireNonNull(accountingDate, "accountingDate must not be null");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        if (slipNo != null) {
            slipNo = slipNo.trim();
            if (slipNo.isEmpty()) {
                throw new IllegalArgumentException("slipNo must not be blank when supplied");
            }
            if (slipNo.length() > 20) {
                throw new IllegalArgumentException("slipNo must not exceed 20 characters");
            }
        }
    }

    /**
     * Compatibility constructor for callers that let journal-ledger allocate a slip number.
     */
    public JournalEntryCommand(
            LocalDate slipDate,
            LocalDate accountingDate,
            String description,
            String entryType,
            String currencyCode,
            BigDecimal exchangeRate,
            String createdBy,
            String auditUser,
            String lineageSourceType,
            String lineageSourceId,
            List<JournalLineCommand> lines) {
        this(
                slipDate,
                accountingDate,
                description,
                entryType,
                currencyCode,
                exchangeRate,
                createdBy,
                auditUser,
                lineageSourceType,
                lineageSourceId,
                null,
                lines);
    }
}
