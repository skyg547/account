package com.ho.account.deposit.application.service;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositStatus;
import com.ho.account.deposit.infrastructure.adapter.out.persistence.DepositAccountPersistenceAdapter;
import com.ho.account.deposit.infrastructure.adapter.out.persistence.SpringDataDepositAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [애플리케이션 서비스 - 동시성 및 낙관적 잠금 재시도 검증 테스트]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 테스트는 Multi-thread 환경에서 여러 쓰레드가 동일한 계좌에 동시 입금을 수행할 때
 * 낙관적 잠금(@Version)과 재시도(Retry) 메커니즘이 원활히 작용하여
 * 갱신 손실(Lost Update) 없이 모든 입금이 누락 없이 반영되는지 검증합니다.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=15"
})
@ContextConfiguration(classes = DepositServiceConcurrencyTest.TestApplication.class)
@Import(DepositAccountPersistenceAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DepositServiceConcurrencyTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = DepositAccount.class)
    @EnableJpaRepositories(basePackageClasses = SpringDataDepositAccountRepository.class)
    static class TestApplication {}

    @Autowired
    private DepositAccountPersistencePort persistencePort;

    @MockBean
    private DepositAccountMappingPort mappingPort;
    @MockBean
    private MasterDataQueryPort masterDataQueryPort;
    @MockBean
    private JournalPostingPort journalPostingPort;

    @Test
    @DisplayName("동시 10개의 입금 요청 시 낙관적 잠금 및 재시도 메커니즘을 통해 갱신 손실 없이 잔액 정합성을 유지한다")
    void concurrentDepositsMaintainBalanceIntegrity() throws InterruptedException {
        DepositService service = new DepositService(
                persistencePort,
                mappingPort,
                masterDataQueryPort,
                journalPostingPort
        );

        DepositAccount account = new DepositAccount();
        account.setAccountNumber("DEP-CONC-001");
        account.setCustomerCode("CUST-CONC");
        account.setProductCode("PROD-001");
        account.setCurrencyCode("KRW");
        account.setInterestRate(new BigDecimal("0.025"));
        account.setStatus(DepositStatus.ACTIVE);
        account.setOpenedAt(LocalDate.now());
        account.deposit(new BigDecimal("10000.00"));
        persistencePort.save(account);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    service.deposit("DEP-CONC-001", new BigDecimal("1000.00"));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        DepositAccount updatedAccount = persistencePort.findByAccountNumber("DEP-CONC-001").orElseThrow();
        // 초기 10,000 + (10 * 1,000) = 20,000
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("20000.00");
    }
}
