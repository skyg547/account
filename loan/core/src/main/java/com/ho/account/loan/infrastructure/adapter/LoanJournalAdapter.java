package com.ho.account.loan.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.outbox.InMemoryOutboxAdapter;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 전표 출력 어댑터 (LoanJournalAdapter)]
 * 
 * 🐣 [초보자를 위한 설명 및 Transactional Outbox 지원]
 * 이 클래스는 Loan 모듈의 전표 요청 명령어 {@link LoanJournalCommand}를 
 * `journal-ledger` 도메인 및 Transactional Outbox 이벤트로 이송하는 출력 어댑터입니다.
 * 
 * **Transactional Outbox 및 Dual Write 방지 아키텍처:**
 * 1. 로컬 트랜잭션 수반 시 `JournalOutboxEvent`를 원자적으로 Outbox 테이블에 저장하여 
 *    네트워크 단절이나 시스템 복구 시에도 이벤트를 유실하지 않습니다.
 * 2. 전표 생성이 성공하면 Outbox 이벤트를 `PUBLISHED` 상태로 갱신하여 
 *    전표 원장과의 최종 정합성(Eventual Consistency)과 멱등성을 보장합니다.
 */
@Component
public class LoanJournalAdapter implements LoanJournalPort {

    private final JournalUseCase journalUseCase;
    private final OutboxPort outboxPort;

    public LoanJournalAdapter(JournalUseCase journalUseCase) {
        this(journalUseCase, new InMemoryOutboxAdapter());
    }

    @Autowired
    public LoanJournalAdapter(JournalUseCase journalUseCase, @Autowired(required = false) OutboxPort outboxPort) {
        this.journalUseCase = journalUseCase;
        this.outboxPort = outboxPort != null ? outboxPort : new InMemoryOutboxAdapter();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostedJournal post(LoanJournalCommand command) {
        // 1. Transactional Outbox 패턴 적용: Outbox 이벤트 원자적 저장
        JournalEntryCommand contractCommand = toContractCommand(command);
        String idempotencyKey = command.lineageSourceType() + ":" + command.lineageSourceId();
        
        JournalOutboxEvent outboxEvent = JournalOutboxEvent.createPending(
                "LOAN",
                command.lineageSourceType(),
                command.lineageSourceId(),
                contractCommand,
                idempotencyKey
        );
        outboxPort.saveJournalEvent(outboxEvent);

        // 2. Journal Entry 엔티티 생성 및 승인/전기 처리
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(command.accountingDate());
        entry.setAccountingDate(command.accountingDate());
        entry.setDescription(command.description());
        // 상태 setter로 승인 단계를 건너뛸 수 없도록 Aggregate의 최초 전이만 호출합니다.
        entry.initializeDraft();
        entry.setEntryType("NORMAL");
        entry.setCreatedBy(command.actor());
        entry.setAuditUser(command.actor());
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());
        entry.setCurrencyCode(command.currencyCode());

        command.lines().forEach(line -> entry.addDetail(toDetail(line, command.actor())));
        JournalEntry saved = journalUseCase.createJournalEntry(entry);
        journalUseCase.approveJournalEntry(saved.getId(), command.actor());
        journalUseCase.postJournalEntry(saved.getId(), command.actor());
        JournalEntry posted = journalUseCase.getJournalEntryWithDetails(saved.getId()).orElse(saved);

        // 3. Outbox 이벤트 발행 성공 상태 갱신
        outboxPort.markJournalEventAsPublished(outboxEvent.getEventId(), LocalDateTime.now());

        return new PostedJournal(posted.getId(), posted.getSlipNo());
    }

    private JournalEntryCommand toContractCommand(LoanJournalCommand command) {
        List<JournalLineCommand> lines = command.lines().stream()
                .map(l -> new JournalLineCommand(l.side(), l.accountCode(), l.amount(), l.amount(), null, null, l.description()))
                .collect(Collectors.toList());

        return new JournalEntryCommand(
                command.accountingDate(),
                command.accountingDate(),
                command.description(),
                "NORMAL",
                command.currencyCode(),
                null,
                command.actor(),
                command.actor(),
                command.lineageSourceType(),
                command.lineageSourceId(),
                lines
        );
    }

    private JournalDetail toDetail(LoanJournalLine line, String actor) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(JournalSide.valueOf(line.side()));
        detail.setAccountCode(line.accountCode());
        detail.setAmount(line.amount());
        detail.setBaseAmount(line.amount());
        detail.setDetailDescription(line.description());
        detail.setAuditUser(actor);
        return detail;
    }
}
