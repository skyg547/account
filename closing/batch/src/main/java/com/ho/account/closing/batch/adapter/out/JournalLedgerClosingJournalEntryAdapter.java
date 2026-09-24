package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 마감 결산 전표 연동 아웃바운드 어댑터 (JournalLedgerClosingJournalEntryAdapter)]
 *
 * ───────────────────────────────────────────────────────────────────────────────────
 * 🐣 [초보자를 위한 아키텍처 및 MSA 전환 교육용 주석 (Pedagogical Comments)]
 *
 * 1. Bounded Context 경계 보존 (Domain Driven Design):
 *    - 마감(Closing) Bounded Context와 분개장(Journal Ledger) Bounded Context는
 *      서로 다른 비즈니스 도메인과 생명주기를 가집니다.
 *    - 기존에는 closing 모듈이 journal-ledger 모듈의 도메인 엔티티(JournalEntry, JournalDetail) 및
 *      내부 서비스(JournalUseCase)를 직접 참조하여 컴파일 타임 강결합이 발생했습니다.
 *    - 리팩토링 후, `:contracts` 공유 커널(Shared Kernel) 모듈의 포트(JournalPostingPort, JournalQueryPort) 및
 *      계약 DTO(JournalEntryCommand, JournalSummary, JournalDetailSummary)만 참조하도록 전환했습니다.
 *
 * 2. 컴파일 타임 격리 (Compile-Time Isolation) & MSA 독립 배포:
 *    - `journal-ledger:core`에 대한 직접 프로젝트 의존성을 완전히 제거함으로써,
 *      두 모듈 간 컴파일 타임 결합이 차단되었습니다.
 *    - 향후 `journal-ledger`가 별도의 독립된 마이크로서비스(Microservice)로 분리되어
 *      REST API 또는 gRPC / Async Kafka Message 전달 방식으로 변경되더라도
 *      `closing` 모듈의 코드 변경 없이 어댑터의 런타임 구현체만 교체할 수 있습니다.
 *
 * 3. 헥사고날 아키텍처 포트/어댑터 패턴 (Hexagonal Architecture):
 *    - `ClosingJournalEntryPort`는 Closing 도메인 관점의 아웃바운드 포트입니다.
 *    - 본 클래스는 해당 포트 인터페이스를 구현하며, 내부적으로 계약 모듈의 포트(`JournalPostingPort`, `JournalQueryPort`)를
 *      조율(Orchestration)하여 마감 전표 생성, 재시도 멱등성 검사 및 승인/전기 요청을 위임합니다.
 * ───────────────────────────────────────────────────────────────────────────────────
 */
@Component
@RequiredArgsConstructor
public class JournalLedgerClosingJournalEntryAdapter implements ClosingJournalEntryPort {

    private final JournalPostingPort journalPostingPort;
    private final JournalQueryPort journalQueryPort;

    @Override
    public ClosingJournalEntryResult createDraftAdjustment(ClosingJournalEntryCommand command) {
        Optional<JournalSummary> existingOpt = journalQueryPort.findBySlipNo(command.slipNo());
        if (existingOpt.isPresent()) {
            JournalSummary existing = existingOpt.get();
            List<JournalDetailSummary> details = journalQueryPort.getJournalDetails(existing.getId());
            requireSameClosingRequest(command, existing, details);
            return new ClosingJournalEntryResult(existing.getId(), existing.getSlipNo());
        }

        JournalEntryCommand journalCommand = new JournalEntryCommand(
                command.slipDate(),
                command.accountingDate(),
                command.description(),
                command.entryType(),
                command.currencyCode(),
                command.exchangeRate(),
                command.createdBy(),
                command.auditUser(),
                command.lineageSourceType(),
                command.lineageSourceId(),
                command.slipNo(),
                command.lines().stream().map(this::toJournalLine).toList());
        JournalPostingResult result = journalPostingPort.createDraftEntry(journalCommand);
        return new ClosingJournalEntryResult(result.journalEntryId(), result.slipNo());
    }

    @Override
    public void approveAndPost(Long journalEntryId, String actor) {
        journalPostingPort.approveAndPost(journalEntryId, actor);
    }

    private JournalLineCommand toJournalLine(ClosingJournalLineCommand line) {
        return new JournalLineCommand(
                line.side() == ClosingJournalSide.DEBIT ? "DEBIT" : "CREDIT",
                line.accountCode(),
                line.amount(),
                line.baseAmount(),
                null,
                null,
                line.description());
    }

    private void requireSameClosingRequest(
            ClosingJournalEntryCommand command,
            JournalSummary persisted,
            List<JournalDetailSummary> details) {
        boolean sameHeader = Objects.equals(command.slipNo(), persisted.getSlipNo())
                && Objects.equals(command.slipDate(), persisted.getSlipDate())
                && Objects.equals(command.accountingDate(), persisted.getAccountingDate())
                && Objects.equals(command.description(), persisted.getDescription())
                && Objects.equals(command.entryType(), persisted.getEntryType())
                && Objects.equals(command.currencyCode(), persisted.getCurrencyCode())
                && Objects.equals(command.lineageSourceType(), persisted.getLineageSourceType())
                && Objects.equals(command.lineageSourceId(), persisted.getLineageSourceId());
        List<String> requestedLines = command.lines().stream()
                .map(this::canonicalLine)
                .sorted()
                .toList();
        List<String> persistedLines = details.stream()
                .map(this::canonicalLine)
                .sorted()
                .toList();
        if (!sameHeader || !requestedLines.equals(persistedLines)) {
            throw new IllegalStateException(
                    "Closing slip already exists with different business content: " + command.slipNo());
        }
        if ("REJECTED".equals(persisted.getStatus())
                || "REVERSED".equals(persisted.getStatus())) {
            throw new IllegalStateException(
                    "Closing slip exists in a non-reusable status " + persisted.getStatus()
                            + ": " + command.slipNo());
        }
    }

    private String canonicalLine(ClosingJournalLineCommand line) {
        return line.side().name()
                + "|" + line.accountCode()
                + "|" + decimal(line.amount())
                + "|" + decimal(line.baseAmount())
                + "|||" + optional(line.description());
    }

    private String canonicalLine(JournalDetailSummary line) {
        return line.getSide().name()
                + "|" + line.getAccountCode()
                + "|" + decimal(line.getAmount())
                + "|" + decimal(line.getBaseAmount())
                + "|" + optional(line.getDepartmentCode())
                + "|" + optional(line.getBusinessPartnerCode())
                + "|" + optional(line.getDetailDescription());
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
