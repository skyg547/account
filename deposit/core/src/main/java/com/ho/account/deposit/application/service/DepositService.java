package com.ho.account.deposit.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.outbox.InMemoryOutboxAdapter;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.JournalOutboxRelayService;
import com.ho.account.contracts.outbox.OutboxEventPublisher;
import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 
 * 🐣 [초보자를 위한 설명 및 MSA 아키텍처 개편]
 * 이 클래스는 예금(Deposit) 모듈의 핵심 유즈케이스 처리기입니다.
 * 
 * **Transactional Outbox 패턴 기반 전표 동기화 (Dual Write 정합성 해결):**
 * As-Is: 계좌 개설 저장 후 외부 Journal Ledger API를 직접 동기 호출.
 *       로컬 DB Commit 후 네트워크 오류로 외부 호출이 실패하면 불일치 발생.
 * To-Be: 계좌 개설 로컬 DB 트랜잭션 내에서 `JournalOutboxEvent`를 원자적(Atomically)으로 Outbox 저장소에 기록합니다.
 *       이후 `OutboxEventPublisher` (비동기 릴레이 엔진)가 PENDING 상태 이벤트를 읽어서 
 *       외부 Journal 시스템({@link JournalPostingPort})에 안전하게 릴레이(Publish)하여 최종 정합성을 확보합니다.
 */
@Service
public class DepositService implements OpenAccountUseCase {

    private final DepositAccountPersistencePort depositAccountPersistencePort;
    private final DepositAccountMappingPort depositAccountMappingPort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final OutboxPort outboxPort;
    private final OutboxEventPublisher outboxEventPublisher;

    public DepositService(DepositAccountPersistencePort depositAccountPersistencePort,
                          DepositAccountMappingPort depositAccountMappingPort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort) {
        this(depositAccountPersistencePort, depositAccountMappingPort, masterDataQueryPort,
                journalPostingPort, new InMemoryOutboxAdapter(), null);
    }

    public DepositService(DepositAccountPersistencePort depositAccountPersistencePort,
                          DepositAccountMappingPort depositAccountMappingPort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort,
                          OutboxPort outboxPort,
                          OutboxEventPublisher outboxEventPublisher) {
        this.depositAccountPersistencePort = depositAccountPersistencePort;
        this.depositAccountMappingPort = depositAccountMappingPort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.outboxPort = outboxPort != null ? outboxPort : new InMemoryOutboxAdapter();
        this.outboxEventPublisher = outboxEventPublisher != null ? outboxEventPublisher
                : new JournalOutboxRelayService(this.outboxPort, this.journalPostingPort);
    }

    @Override
    @Transactional
    public String openAccount(OpenAccountCommand command) {
        String accountNumber = generateAccountNumber();
        LocalDate openedAt = LocalDate.now();
        
        DepositAccount account = new DepositAccount();
        account.setAccountNumber(accountNumber);
        account.setCustomerCode(command.customerCode());
        account.setProductCode(command.productCode());
        account.setCurrencyCode(command.currencyCode());
        account.setInterestRate(command.interestRate());
        account.setStatus(DepositStatus.ACTIVE);
        account.setOpenedAt(openedAt);
        account.setValidFrom(openedAt);
        account.setValidTo(LocalDate.of(9999, 12, 31));
        
        // 초기 입금액 처리를 도메인 메서드를 통해 수행하여 무결성 보장
        if (hasInitialDeposit(command.initialDeposit())) {
            account.deposit(command.initialDeposit());
        }
        
        DepositAccount savedAccount = depositAccountPersistencePort.save(account);

        if (hasInitialDeposit(command.initialDeposit())) {
            postInitialDepositJournalWithOutbox(savedAccount, command.initialDeposit());
        }
        
        return accountNumber;
    }

    private boolean hasInitialDeposit(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    /**
     * [Transactional Outbox 기반 전표 발행 이송]
     * 로컬 트랜잭션 안에서 JournalEntryCommand를 포함한 JournalOutboxEvent를 원자적으로 저장하고,
     * Outbox 릴레이 서비스를 통해 전표 서비스에 이벤트를 발행합니다.
     */
    private void postInitialDepositJournalWithOutbox(DepositAccount account, BigDecimal amount) {
        DepositAccountMappingPort.InitialDepositAccounts accounts =
                depositAccountMappingPort.resolveInitialDepositAccounts(account);
        requireAccounts(accounts.requiredAccountCodes());

        JournalEntryCommand command = new JournalEntryCommand(
                account.getOpenedAt(),
                account.getOpenedAt(),
                "Initial deposit: " + account.getAccountNumber(),
                "DEPOSIT_INITIAL_DEPOSIT",
                account.getCurrencyCode(),
                null,
                resolveActor(account),
                resolveActor(account),
                "DEPOSIT_ACCOUNT",
                account.getAccountNumber(),
                List.of(
                        new JournalLineCommand("DEBIT", accounts.cashAccountCode(), amount, amount, null,
                                account.getCustomerCode(), "Initial cash deposit"),
                        new JournalLineCommand("CREDIT", accounts.depositLiabilityAccountCode(), amount, amount, null,
                                account.getCustomerCode(), "Deposit liability recognized")));

        // 1. Transactional Outbox 이벤트 저장 (동일 로컬 DB 트랜잭션 내 원자적 저장)
        JournalOutboxEvent outboxEvent = JournalOutboxEvent.createPending(
                "DEPOSIT",
                "DEPOSIT_ACCOUNT",
                account.getAccountNumber(),
                command,
                "DEPOSIT_ACCOUNT:" + account.getAccountNumber()
        );
        outboxPort.saveJournalEvent(outboxEvent);

        // 2. 비동기/동기 릴레이 엔진을 통한 외부 전달 (At-Least-Once Delivery & Eventual Consistency)
        outboxEventPublisher.publish(outboxEvent);
    }

    private void requireAccounts(List<String> accountCodes) {
        for (String accountCode : accountCodes) {
            masterDataQueryPort.findAccountSubject(accountCode)
                    .orElseThrow(() -> new IllegalStateException("Account missing: " + accountCode));
        }
    }

    private String resolveActor(DepositAccount account) {
        if (account.getCreatedBy() != null && !account.getCreatedBy().isBlank()) {
            return account.getCreatedBy().trim();
        }
        return "SYSTEM";
    }

    private String generateAccountNumber() {
        return "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
