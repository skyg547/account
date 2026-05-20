package com.ho.account.deposit.application.service;

import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
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

    @Override
    @Transactional
    public String openAccount(OpenAccountCommand command) {
        String accountNumber = generateAccountNumber();
        
        DepositAccount account = new DepositAccount();
        account.setAccountNumber(accountNumber);
        account.setCustomerCode(command.customerCode());
        account.setProductCode(command.productCode());
        account.setCurrencyCode(command.currencyCode());
        account.setInterestRate(command.interestRate());
        
        // 초기 입금액 처리를 도메인 메서드를 통해 수행하여 무결성 보장
        if (command.initialDeposit() != null && command.initialDeposit().signum() > 0) {
            account.deposit(command.initialDeposit());
        }
        
        account.setStatus(DepositStatus.ACTIVE);
        account.setOpenedAt(LocalDate.now());
        account.setValidFrom(LocalDate.now());
        account.setValidTo(LocalDate.of(9999, 12, 31));
        
        depositAccountPersistencePort.save(account);
        
        // TODO: Create Journal Entry for initial deposit via JournalUseCase Port
        
        return accountNumber;
    }

    private String generateAccountNumber() {
        // Prototype용 임시 난수 계좌번호 생성기
        return "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

