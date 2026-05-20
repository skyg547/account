package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 영속성 어댑터 (Persistence Adapter)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 애플리케이션의 '포트(Port)'와 실제 '데이터베이스(JPA)' 사이를 연결해주는 '변환기(Adapter)'입니다.
 * 도메인 계층(DepositService)은 오직 DepositAccountPersistencePort 인터페이스만 바라보며, 
 * 이 어댑터가 뒤에서 몰래 Spring Data JPA 리포지토리를 호출하여 데이터를 저장하고 가져오는 궂은일을 대신합니다.
 * 이렇게 분리하면 나중에 DB 기술이 바뀌어도 도메인 로직은 단 한 줄도 수정할 필요가 없습니다.
 */
@Component
@RequiredArgsConstructor
public class DepositAccountPersistenceAdapter implements DepositAccountPersistencePort {

    private final SpringDataDepositAccountRepository repository;

    @Override
    public DepositAccount save(DepositAccount depositAccount) {
        return repository.save(depositAccount);
    }

    @Override
    public Optional<DepositAccount> findByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber);
    }
}
