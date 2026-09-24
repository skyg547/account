package com.ho.account.journalledger.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalActor;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 전표 전기 인바운드/아웃바운드 포트 어댑터 (JournalPostingAdapter)]
 *
 * ───────────────────────────────────────────────────────────────────────────────────
 * 🐣 [초보자를 위한 아키텍처 및 MSA 전환 교육용 주석 (Pedagogical Comments)]
 * 
 * 1. 모놀리스 직접 결합(Monolith Direct Coupling)의 한계와 탈피:
 *    - 기존 As-Is 구조: 타 모듈(Receivable, Payable, Loan, Deposit 등)이 journal-ledger 모듈 내부의
 *      `MonolithJournalPostingAdapter`나 `JournalUseCase` 서비스 Bean을 직접 주입받아 Java 메서드를 호출했습니다.
 *    - 문제점: 이 방식은 Monolith Single-Process 환경에서는 편해 보이지만, 모듈 간 컴파일 타임 강결합을 유발하여
 *      `journal-ledger`를 독립적인 Microservice(Bounded Context)로 분리/배포하는 데 심각한 걸림돌이 됩니다.
 *
 * 2. 헥사고날 포트/어댑터 패턴 (Hexagonal Architecture Port & Adapter)을 통한 디커플링:
 *    - To-Be 구조: 모듈 간 계약(Contracts) 모듈에 독립된 `JournalPostingPort` 인터페이스와
 *      `JournalEntryCommand` / `JournalPostingResult` DTO 표준을 정립했습니다.
 *    - 이 클래스(`JournalPostingAdapter`)는 `JournalPostingPort`를 구현하여 외부 모듈의 요청을 수신하고,
 *      내부 도메인/유즈케이스(`JournalUseCase`)로 변환 전달하는 헥사고날 어댑터 역할을 수행합니다.
 *    - 결과적으로 호출 측(도메인 서비스)은 `JournalPostingPort` 인터페이스에만 의존하며,
 *      배포 환경에 따라 In-Memory 로컬 Bean 호출, REST HTTP Client, Async Messaging(Kafka/Outbox) 등으로
 *      호출 방식을 자유롭게 전환할 수 있습니다.
 *
 * 3. Transactional Outbox 및 멱등성(Idempotency) 보장:
 *    - MSA 분리 환경에서 네트워크 유실 및 At-Least-Once Delivery로 인한 메시지 중복 수신이 발생할 수 있습니다.
 *    - 본 어댑터는 수신된 `JournalEntryCommand`의 `lineageSourceType`과 `lineageSourceId`를 기반으로 
 *      이미 발행된 동일 원천 전표가 존재하는지 멱등성 검사(Idempotency Check)를 수행합니다.
 *    - 중복 요청 시 기존 전표 식별자 및 상태를 반환하여 중복 전표가 생기는 것을 방지합니다.
 * ───────────────────────────────────────────────────────────────────────────────────
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

        // 2. 신규 전표 엔티티 조립 (계약 커맨드 -> journal-ledger 도메인 모델 변환)
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(command.accountingDate());
        entry.setSlipDate(command.slipDate());
        entry.setDescription(command.description());
        entry.setEntryType(command.entryType());
        entry.setExchangeRate(command.exchangeRate() != null ? command.exchangeRate() : BigDecimal.ONE);
        String maker = JournalActor.canonicalize(command.createdBy());
        entry.setCreatedBy(maker);
        // Contract payload auditUser is not an independent identity source at draft creation.
        entry.setAuditUser(maker);
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());
        if (command.slipNo() != null && !command.slipNo().isBlank()) {
            entry.setSlipNo(command.slipNo().trim());
        }

        // 기본 통화 KRW (Command에 없으면)
        String currencyCode = command.currencyCode() != null ? command.currencyCode() : "KRW";
        entry.setCurrencyCode(currencyCode);

        // 분개 상세(JournalDetail) 라인 생성
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
            detail.setAuditUser(maker);
            detail.setJournalEntry(entry);
            return detail;
        }).collect(Collectors.toList()));

        // 3. 유즈케이스 호출 및 생성 결과 반환
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        
        return new JournalPostingResult(
                savedEntry.getId(),
                savedEntry.getSlipNo(),
                savedEntry.getStatus().name()
        );
    }

    @Override
    @Transactional
    public void approveAndPost(Long journalEntryId, String actor) {
        JournalEntry entry = journalUseCase.getJournalEntry(journalEntryId)
                .orElseThrow(() -> new IllegalStateException(
                        "Closing journal entry not found: " + journalEntryId));
        String checker = JournalActor.canonicalize(actor);
        JournalEntryStatus status = entry.getStatus();
        if (status == JournalEntryStatus.POSTED) {
            return;
        }
        if (status == JournalEntryStatus.DRAFT) {
            if (JournalActor.sameIdentity(entry.getCreatedBy(), checker)) {
                throw new IllegalStateException("Machine journal maker and checker principals must be distinct.");
            }
            // A machine draft still enters the same controlled state; the persisted maker submits it.
            journalUseCase.requestJournalEntryApproval(journalEntryId, entry.getCreatedBy());
            journalUseCase.approveJournalEntry(journalEntryId, checker);
            journalUseCase.postJournalEntry(journalEntryId, checker);
            return;
        }
        if (status == JournalEntryStatus.REQUESTED) {
            journalUseCase.approveJournalEntry(journalEntryId, checker);
            journalUseCase.postJournalEntry(journalEntryId, checker);
            return;
        }
        if (status == JournalEntryStatus.APPROVED) {
            journalUseCase.postJournalEntry(journalEntryId, checker);
            return;
        }
        throw new IllegalStateException(
                "Closing journal cannot be posted from status " + status + ": " + journalEntryId);
    }
}
