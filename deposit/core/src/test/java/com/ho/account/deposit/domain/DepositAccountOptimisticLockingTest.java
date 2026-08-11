package com.ho.account.deposit.domain;

import com.ho.account.deposit.infrastructure.adapter.out.persistence.SpringDataDepositAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [도메인/영속성 - 낙관적 잠금(Optimistic Locking) 검증 테스트]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 테스트 클래스는 @Version 필드를 통한 JPA 낙관적 잠금이 정상적으로 작동하는지 검증합니다.
 * 동일한 계좌를 동시 수정하려 할 때 갱신 손실(Lost Update)을 방지하고 예외를 던지는지 확인합니다.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = DepositAccountOptimisticLockingTest.TestApplication.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DepositAccountOptimisticLockingTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = DepositAccount.class)
    @EnableJpaRepositories(basePackageClasses = SpringDataDepositAccountRepository.class)
    static class TestApplication {}

    @Autowired
    private SpringDataDepositAccountRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("DepositAccount 엔티티 수정 시 @Version 필드가 1씩 자동 증가한다")
    void versionIncrementsOnUpdate() {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);

        Long id = tt.execute(status -> {
            DepositAccount account = new DepositAccount();
            account.setAccountNumber("DEP-OPT-001");
            account.setCustomerCode("CUST-001");
            account.setProductCode("PROD-001");
            account.setCurrencyCode("KRW");
            account.setInterestRate(new BigDecimal("0.025"));
            account.setStatus(DepositStatus.ACTIVE);
            account.setOpenedAt(LocalDate.now());
            return repository.save(account).getId();
        });

        tt.executeWithoutResult(status -> {
            DepositAccount account = repository.findById(id).orElseThrow();
            Long initialVersion = account.getVersion();
            assertThat(initialVersion).isNotNull();

            account.deposit(new BigDecimal("1000.00"));
            repository.save(account);
        });

        tt.executeWithoutResult(status -> {
            DepositAccount account = repository.findById(id).orElseThrow();
            assertThat(account.getVersion()).isEqualTo(1L);
        });
    }

    @Test
    @DisplayName("동일한 버전을 가진 두 엔티티 동시 수정 시 갱신 손실(Lost Update) 대신 OptimisticLockingFailureException이 발생한다")
    void optimisticLockingPreventsLostUpdate() {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);

        Long id = tt.execute(status -> {
            DepositAccount account = new DepositAccount();
            account.setAccountNumber("DEP-OPT-002");
            account.setCustomerCode("CUST-002");
            account.setProductCode("PROD-001");
            account.setCurrencyCode("KRW");
            account.setInterestRate(new BigDecimal("0.025"));
            account.setStatus(DepositStatus.ACTIVE);
            account.setOpenedAt(LocalDate.now());
            return repository.save(account).getId();
        });

        DepositAccount entity1 = tt.execute(status -> repository.findById(id).orElseThrow());
        DepositAccount entity2 = tt.execute(status -> repository.findById(id).orElseThrow());

        tt.executeWithoutResult(status -> {
            entity1.deposit(new BigDecimal("5000.00"));
            repository.save(entity1);
        });

        assertThatThrownBy(() -> {
            tt.executeWithoutResult(status -> {
                entity2.deposit(new BigDecimal("3000.00"));
                repository.save(entity2);
            });
        }).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
