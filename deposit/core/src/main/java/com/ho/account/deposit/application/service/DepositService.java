package com.ho.account.deposit.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템의 '지휘자' 역할을 합니다.
 * "계좌 개설해줘!"라는 요청(UseCase)이 들어오면,
 * 1. 계좌 번호를 만들고
 * 2. 계좌 도메인 객체(DepositAccount)를 생성해서 값을 채운 뒤
 * 3. 영속성 포트(DepositAccountPersistencePort)에게 "DB에 저장해!" 라고 지시합니다.
 * 핵심 비즈니스 로직(입금/출금 등)은 도메인 객체 내부에 위임하고, 서비스는 흐름만 제어합니다.
 */
@Service
@RequiredArgsConstructor
public class DepositService implements OpenAccountUseCase {

    // JPA Repository 대신 아웃바운드 포트(인터페이스)에 의존합니다. (DIP: 의존성 역전 원칙)
    private final DepositAccountPersistencePort depositAccountPersistencePort;
    private final DepositAccountMappingPort depositAccountMappingPort;
    private final MasterDataQueryPort masterDataQueryPort;
    private final JournalPostingPort journalPostingPort;

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
            postInitialDepositJournal(savedAccount, command.initialDeposit());
        }
        
        return accountNumber;
    }

    private boolean hasInitialDeposit(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    private void postInitialDepositJournal(DepositAccount account, BigDecimal amount) {
        DepositAccountMappingPort.InitialDepositAccounts accounts =
                depositAccountMappingPort.resolveInitialDepositAccounts(account);
        requireAccounts(accounts.requiredAccountCodes());

        journalPostingPort.createDraftEntry(new JournalEntryCommand(
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
                                account.getCustomerCode(), "Deposit liability recognized"))));
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
        // Prototype용 임시 난수 계좌번호 생성기
        return "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

