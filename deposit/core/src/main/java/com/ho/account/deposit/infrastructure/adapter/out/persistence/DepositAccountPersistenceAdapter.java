package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 영속성 어댑터 (Persistence Adapter)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 애플리케이션의 '포트(Port)'와 실제 '데이터베이스(JPA)' 사이를 연결해주는 '변환기(Adapter)'입니다.
 * 도메인 계층(DepositService)은 오직 DepositAccountPersistencePort 인터페이스만 바라보며, 
 * 이 어댑터가 뒤에서 Spring Data JPA 리포지토리를 호출하여 데이터를 저장하고 가져오는 역할을 수행합니다.
 *
 * 📌 [동시성 제어 - 낙관적 잠금 예외 전파 구조]
 * Spring Data JPA의 {@code repository.save()} 호출 시, JPA엔티티({@link DepositAccount})의 {@code @Version} 필드가
 * 기존 DB 상의 버전과 다를 경우 JPA/Hibernate 레벨에서 {@code ObjectOptimisticLockingFailureException} 또는
 * Spring의 추상화 예외인 {@link org.springframework.dao.OptimisticLockingFailureException}을 발생시킵니다.
 * 영속성 어댑터는 이 예외를 상위 유즈케이스(DepositService)로 투명하게 전파하여,
 * 비즈니스 레이어가 안전하게 재시도(Retry) 메커니즘을 적용할 수 있도록 지원합니다.
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

    @Override
    public List<DepositAccount> findActiveAccounts(LocalDate asOfDate) {
        return repository.findByStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
                DepositStatus.ACTIVE, asOfDate, asOfDate);
    }
}
