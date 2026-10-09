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
import com.ho.account.deposit.application.port.in.DepositQueryUseCase;
import com.ho.account.deposit.application.port.in.DepositTransactionUseCase;
import com.ho.account.deposit.application.port.in.DepositUseCase;
import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 
 * 🐣 [초보자를 위한 설명 및 MSA 아키텍처 개편]
 * 이 클래스는 예금(Deposit) 모듈의 핵심 유즈케이스 처리기입니다.
 * 인바운드 유즈케이스 포트인 {@link DepositUseCase} ({@link OpenAccountUseCase}, {@link DepositTransactionUseCase}, {@link DepositQueryUseCase})를 구현하여
 * Spring Container에 `@Service` 빈으로 자동 등록됩니다.
 * 
 * **Transactional Outbox 패턴 기반 전표 동기화 (Dual Write 정합성 해결):**
 * As-Is: 계좌 개설 저장 후 외부 Journal Ledger API를 직접 동기 호출.
 *       로컬 DB Commit 후 네트워크 오류로 외부 호출이 실패하면 불일치 발생.
 * To-Be: 계좌 개설 로컬 DB 트랜잭션 내에서 `JournalOutboxEvent`를 원자적(Atomically)으로 Outbox 저장소에 기록합니다.
 *       이후 `OutboxEventPublisher` (비동기 릴레이 엔진)가 PENDING 상태 이벤트를 읽어서 
 *       외부 Journal 시스템({@link JournalPostingPort})에 안전하게 릴레이(Publish)하여 최종 정합성을 확보합니다.
 *
 * **낙관적 잠금(Optimistic Locking) 기반 동시성 입출금 처리 및 재시도 메커니즘:**
 * 금융 예금 계좌에서 동시 입출금 요청 시 발생할 수 있는 갱신 손실(Lost Update)을 방지하기 위해
 * JPA `@Version` 기반 낙관적 잠금을 적용하고, 충돌 발생 시 최신 엔티티 재조회 후 재시도(Retry)합니다.
 */
@Service
public class DepositService implements DepositUseCase {

    private final DepositAccountPersistencePort depositAccountPersistencePort;
    private final DepositAccountMappingPort depositAccountMappingPort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;
    private final OutboxPort outboxPort;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TransactionTemplate balanceChangeTransaction;

    @Autowired
    public DepositService(DepositAccountPersistencePort depositAccountPersistencePort,
                          DepositAccountMappingPort depositAccountMappingPort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort,
                          OutboxPort outboxPort,
                          @Autowired(required = false) OutboxEventPublisher outboxEventPublisher,
                          PlatformTransactionManager transactionManager) {
        this.depositAccountPersistencePort = depositAccountPersistencePort;
        this.depositAccountMappingPort = depositAccountMappingPort;
        this.masterDataQueryPort = masterDataQueryPort;
        this.journalPostingPort = journalPostingPort;
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort must not be null");
        this.outboxEventPublisher = outboxEventPublisher != null ? outboxEventPublisher
                : new JournalOutboxRelayService(this.outboxPort, this.journalPostingPort);
        this.balanceChangeTransaction = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager must not be null"));
        this.balanceChangeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Compatibility constructor for account-opening tests that do not exercise balance changes. */
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
        this.balanceChangeTransaction = null;
    }

    public DepositService(DepositAccountPersistencePort depositAccountPersistencePort,
                          DepositAccountMappingPort depositAccountMappingPort,
                          MasterDataQueryPort masterDataQueryPort,
                          JournalPostingPort journalPostingPort) {
        this(depositAccountPersistencePort, depositAccountMappingPort, masterDataQueryPort,
                journalPostingPort, new InMemoryOutboxAdapter(), null);
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

    /**
     * [낙관적 잠금(Optimistic Locking) 예외 처리 및 입금 재시도 유즈케이스]
     * 
     * 🐣 [초보자를 위한 설명]
     * 동시 입금 요청 시 JPA @Version 버전 충돌(OptimisticLockingFailureException)이 발생할 경우,
     * DB에서 최신 계좌 정보를 다시 읽어서(Re-fetch) 입금 도메인 로직을 다시 수행합니다.
     * 이를 통해 갱신 손실(Lost Update) 없이 잔액 정합성을 안전하게 유지합니다.
     */
    @Override
    public void deposit(String accountNumber, BigDecimal amount) {
        changeBalance(accountNumber, amount, true);
    }

    /**
     * [낙관적 잠금(Optimistic Locking) 예외 처리 및 출금 재시도 유즈케이스]
     * 
     * 🐣 [초보자를 위한 설명]
     * 동시 출금 요청 시 버전 충돌 발생 시 최신 계좌 잔액을 다시 조회하여 잔액 검증 후 출금을 재시도합니다.
     */
    @Override
    public void withdraw(String accountNumber, BigDecimal amount) {
        changeBalance(accountNumber, amount, false);
    }

    private void changeBalance(String accountNumber, BigDecimal amount, boolean deposit) {
        if (amount == null) {
            throw new IllegalArgumentException("거래 금액은 필수입니다.");
        }
        BigDecimal journalAmount;
        try {
            journalAmount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("거래 금액은 소수 둘째 자리까지만 허용합니다.", ex);
        }
        if (balanceChangeTransaction == null) {
            throw new IllegalStateException("입출금에는 트랜잭션 관리자가 필요합니다.");
        }

        // The operation retains its lineage across retries; every failed attempt rolls back first.
        String transactionId = UUID.randomUUID().toString();
        executeWithOptimisticLockRetry(() -> balanceChangeTransaction.executeWithoutResult(status -> {
            DepositAccount account = depositAccountPersistencePort.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다: " + accountNumber));
            // Foreign-currency journals need a verified FX rate and converted KRW base amounts.
            // This use case has neither input, so reject before changing a balance that cannot be journaled.
            if (!"KRW".equals(account.getCurrencyCode())) {
                throw new IllegalStateException("외화 입출금에는 환율과 기준통화 금액이 필요합니다.");
            }
            if (deposit) {
                account.deposit(journalAmount);
            } else {
                account.withdraw(journalAmount);
            }
            depositAccountPersistencePort.save(account);
            saveBalanceChangeJournal(account, journalAmount, deposit, transactionId);
        }));
    }

    private void saveBalanceChangeJournal(DepositAccount account, BigDecimal amount,
                                          boolean deposit, String transactionId) {
        DepositAccountMappingPort.InitialDepositAccounts accounts =
                depositAccountMappingPort.resolveInitialDepositAccounts(account);
        requireAccounts(accounts.requiredAccountCodes());

        String transactionType = deposit ? "DEPOSIT_DEPOSIT" : "DEPOSIT_WITHDRAWAL";
        String debitAccount = deposit ? accounts.cashAccountCode() : accounts.depositLiabilityAccountCode();
        String creditAccount = deposit ? accounts.depositLiabilityAccountCode() : accounts.cashAccountCode();
        LocalDate accountingDate = LocalDate.now();
        JournalEntryCommand command = new JournalEntryCommand(
                accountingDate,
                accountingDate,
                (deposit ? "Deposit: " : "Withdrawal: ") + account.getAccountNumber(),
                transactionType,
                account.getCurrencyCode(),
                null,
                resolveActor(account),
                resolveActor(account),
                "DEPOSIT_TRANSACTION",
                transactionId,
                List.of(
                        new JournalLineCommand("DEBIT", debitAccount, amount, amount, null,
                                account.getCustomerCode(), transactionType),
                        new JournalLineCommand("CREDIT", creditAccount, amount, amount, null,
                                account.getCustomerCode(), transactionType)));

        // The scheduler relays this PENDING event only after the account and outbox commit together.
        outboxPort.saveJournalEvent(JournalOutboxEvent.createPending(
                "DEPOSIT", "DEPOSIT_TRANSACTION", transactionId, command,
                "DEPOSIT_TRANSACTION:" + transactionId));
    }

    /**
     * [낙관적 잠금 충돌 재시도(Retry) 헬퍼 메서드]
     * 
     * 동시 트랜잭션으로 인한 OptimisticLockingFailureException 발생 시
     * 지정된 최대 횟수(maxAttempts)만큼 지수 백오프(Exponential Backoff) 대기 후 작업을 재시도합니다.
     */
    private void executeWithOptimisticLockRetry(Runnable action) {
        int maxAttempts = 10;
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                action.run();
                break;
            } catch (OptimisticLockingFailureException ex) {
                if (attempt >= maxAttempts) {
                    throw new IllegalStateException("동시성 충돌로 인한 최대 재시도 횟수 초과 (" + maxAttempts + "회)", ex);
                }
                try {
                    Thread.sleep(10L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("재시도 대기 중 인터럽트가 발생하였습니다.", ie);
                }
            }
        }
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
        try {
            outboxEventPublisher.publish(outboxEvent);
        } catch (Exception ex) {
            // 외부 전표 발행 실패는 로컬 트랜잭션을 롤백시키지 않음 (Eventual Consistency 보장)
            // OutboxRelay가 스케줄러를 통해 재시도함.
        }
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

    /**
     * [예금 계좌 단건 조회 유즈케이스 구현]
     *
     * 🐣 [초보자를 위한 설명]
     * 계좌번호를 받아 영속성 어댑터(DepositAccountPersistencePort)를 통해 DB에서 계좌 정보를 조회합니다.
     * readOnly = true 트랜잭션 옵션을 사용하여 변경 감지(Dirty Checking) 비용을 절감하고 성능을 최적화합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<DepositAccount> findByAccountNumber(String accountNumber) {
        return depositAccountPersistencePort.findByAccountNumber(accountNumber);
    }

    private String generateAccountNumber() {
        return "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
