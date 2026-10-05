package com.ho.account.loan.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.outbox.InMemoryOutboxAdapter;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalActor;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.service.LoanAccountingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * [헥사고날 아키텍처 - 전표 출력 어댑터 (LoanJournalAdapter)]
 * 
 * 🐣 [초보자를 위한 설명 및 Outbox 포트]
 * 이 클래스는 Loan 모듈의 전표 요청 명령어 {@link LoanJournalCommand}를
 * `journal-ledger` 도메인에 전달하고 OutboxPort에 전표 이벤트를 기록합니다.
 * 
 * 기본 OutboxPort는 비영속 InMemoryOutboxAdapter입니다. 실패 시 PENDING 이벤트가 메모리에
 * 남을 수 있고 프로세스 재시작 시 사라집니다. 자동 릴레이·재처리나 전표와의 원자성은 보장하지
 * 않습니다. 원자적 저장은 같은 DB 트랜잭션에 참여하는 영속 OutboxPort를 별도로 연결해야 합니다.
 */
@Component
@ConditionalOnProperty(prefix = "account.loan.remote", name = "enabled",
        havingValue = "false", matchIfMissing = true)
public class LoanJournalAdapter implements LoanJournalPort {

    private static final Pattern MACHINE_APPROVER = Pattern.compile("service:[a-z0-9][a-z0-9._-]*");

    private final JournalUseCase journalUseCase;
    private final OutboxPort outboxPort;
    private final LoanAccountingProperties accountingProperties;

    public LoanJournalAdapter(JournalUseCase journalUseCase, LoanAccountingProperties accountingProperties) {
        this(journalUseCase, new InMemoryOutboxAdapter(), accountingProperties);
    }

    @Autowired
    public LoanJournalAdapter(JournalUseCase journalUseCase, @Autowired(required = false) OutboxPort outboxPort,
            @Autowired(required = false) LoanAccountingProperties accountingProperties) {
        this.journalUseCase = Objects.requireNonNull(journalUseCase, "journalUseCase");
        this.outboxPort = outboxPort != null ? outboxPort : new InMemoryOutboxAdapter();
        // Minimal adapter-only contexts may omit configuration; posting still fails closed below.
        this.accountingProperties = accountingProperties != null ? accountingProperties : new LoanAccountingProperties();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostedJournal post(LoanJournalCommand command) {
        Objects.requireNonNull(command, "command");
        // Validate both principals before outbox or journal writes, including canonical maker/checker separation.
        String maker = JournalActor.canonicalize(command.actor());
        String approver = JournalActor.canonicalize(accountingProperties.getJournalApproverActor());
        if (!MACHINE_APPROVER.matcher(approver).matches()) {
            throw new IllegalStateException("account.loan.accounting.journal-approver-actor must be a service principal");
        }
        if (JournalActor.sameIdentity(maker, approver)) {
            throw new IllegalStateException("Loan journal maker and configured approver must differ");
        }
        // 1. 전표 호출 전 이벤트 기록. 기본 메모리 구현은 뒤따르는 실패에 함께 롤백되지 않습니다.
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
        journalUseCase.requestJournalEntryApproval(saved.getId(), command.actor());
        journalUseCase.approveJournalEntry(saved.getId(), approver);
        journalUseCase.postJournalEntry(saved.getId(), command.actor());
        JournalEntry posted = journalUseCase.getJournalEntryWithDetails(saved.getId()).orElse(saved);

        // 3. 로컬 전기 호출 성공을 표시합니다. 메모리 표시만으로 외부 발행을 증명하지 않습니다.
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
