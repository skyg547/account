package com.ho.account.journalledger.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 전표 전기 수신 어댑터 (JournalPostingAdapter)]
 * 
 * 🐣 [초보자를 위한 설명 및 멱등성(Idempotency) 보장 아키텍처]
 * 이 어댑터는 Contracts 모듈의 {@link JournalPostingPort}를 구현하여 
 * 타 마이크로서비스(Deposit, Loan, Payable, Receivable 등)로부터 전표 생성 요청을 수신합니다.
 * 
 * **Transactional Outbox 및 멱등성(Idempotency) 보장:**
 * 1. Transactional Outbox 비동기 릴레이 환경에서는 메시지가 At-Least-Once Delivery로 중복 수신될 수 있습니다.
 * 2. 수신측에서는 `lineageSourceType`과 `lineageSourceId` (또는 idempotencyKey)를 조회하여 
 *    이미 동일한 원천 거래로 전표가 발행된 경우 기존 전표 결과를 멱등(Idempotent)하게 반환하여 중복 전표 생성을 방지합니다.
 */
@Component
@RequiredArgsConstructor
public class JournalPostingAdapter implements JournalPostingPort {

    private final JournalUseCase journalUseCase;

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        // 1. 멱등성(Idempotency) 검사: 동일 lineageSourceType & lineageSourceId 로 이미 발행된 전표가 있는지 확인
        if (command.lineageSourceType() != null && command.lineageSourceId() != null) {
            List<JournalEntry> existingEntries = journalUseCase.getJournalEntriesBySource(
                    command.lineageSourceType(), command.lineageSourceId());
            if (!existingEntries.isEmpty()) {
                JournalEntry existing = existingEntries.get(0);
                return new JournalPostingResult(
                        existing.getId(),
                        existing.getSlipNo(),
                        existing.getStatus().name()
                );
            }
        }

        // 2. 신규 전표 엔티티 조립
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(command.accountingDate());
        entry.setSlipDate(command.slipDate());
        entry.setDescription(command.description());
        entry.setEntryType(command.entryType());
        entry.setExchangeRate(command.exchangeRate() != null ? command.exchangeRate() : BigDecimal.ONE);
        entry.setCreatedBy(command.createdBy());
        entry.setAuditUser(command.auditUser());
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());
        if (command.slipNo() != null && !command.slipNo().isBlank()) {
            entry.setSlipNo(command.slipNo().trim());
        }

        // 기본 통화 KRW (Command에 없으면)
        String currencyCode = command.currencyCode() != null ? command.currencyCode() : "KRW";
        entry.setCurrencyCode(currencyCode);

        entry.setDetails(command.lines().stream().map(line -> {
            JournalDetail detail = new JournalDetail();
            
            detail.setAccountCode(line.accountCode());
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount() != null ? line.baseAmount() : line.amount());
            detail.setSide("DEBIT".equalsIgnoreCase(line.drcrType()) ? JournalSide.DEBIT : JournalSide.CREDIT);
            
            if (line.departmentCode() != null) {
                detail.setDepartmentCode(line.departmentCode());
            }
            
            if (line.businessPartnerCode() != null) {
                detail.setBusinessPartnerCode(line.businessPartnerCode());
            }
            
            detail.setDetailDescription(line.detailDescription());
            detail.setAuditUser(command.auditUser());
            detail.setJournalEntry(entry);
            return detail;
        }).collect(Collectors.toList()));

        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        return new JournalPostingResult(
                savedEntry.getId(),
                savedEntry.getSlipNo(),
                savedEntry.getStatus().name()
        );
    }
}
