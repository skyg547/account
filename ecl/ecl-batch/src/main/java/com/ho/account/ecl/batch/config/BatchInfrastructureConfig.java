package com.ho.account.ecl.batch.config;

import com.ho.account.ecl.batch.support.ColumnRangePartitioner;
import com.ho.account.ecl.batch.support.QuerydslPagingItemReader;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.CrBatchQueryProvider;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.CrBatchQueryProvider.LongRange;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.CrRiskResult;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import javax.sql.DataSource;

/**
 * [Infrastructure] 대손충당금(IFRS9) 배치 공통 인프라 설정 (Batch Infrastructure)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 클래스는 배치 프로그램을 돌리기 위한 '엔진룸'과 같습니다.
 * 배치가 멈췄을 때 어디까지 돌았는지 기억(JobRepository)하거나, 
 * 여러 작업이 꼬이지 않게 관리(TransactionManager)하고, 
 * 한꺼번에 많은 일을 처리하기 위한 '일꾼들(TaskExecutor)'을 설정합니다.
 */
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchInfrastructureConfig {

    /** 데이터 소스: DB 연결을 위한 핵심 객체 */
    private final DataSource dataSource;
    /** 엔티티 매니저 팩토리: JPA 기반의 데이터 접근을 위한 팩토리 */
    private final EntityManagerFactory entityManagerFactory;
    /** JDBC 연산자: 파티셔닝 등 원시 SQL 실행을 위한 도구 */
    private final JdbcOperations jdbcOperations;

    /**
     * [JobRepository] 배치 실행 이력을 관리하는 저장소입니다.
     * 💡 [초보자 가이드] 배치가 성공했는지 시도 중인지 등을 DB 테이블(BATCH_JOB_EXECUTION 등)에 기록하는 역할을 합니다.
     */
    @Bean
    public JobRepository jobRepository(PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * [TransactionManager] 데이터의 일관성을 관리하는 관리자입니다.
     * 💡 [초보자 가이드] 여러 작업을 하나로 묶어, 하나라도 실패하면 전체를 처음 상태로 되돌리는(Rollback) 중요한 역할을 합니다.
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * [TaskExecutor] 병렬 처리를 위한 일꾼 스레드풀입니다.
     * 💡 [초보자 가이드] 수백만 건의 데이터를 혼자서 처리하면 너무 오래 걸리므로, 
     *    여러 명의 일꾼(스레드)이 동시에 나누어 일할 수 있게 해줍니다.
     */
    @Bean
    public TaskExecutor creditRiskTaskExecutor(
            @Value("${spring.profiles.active:default}") String activeProfile) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // [테스트 최적화] 테스트 환경에서는 스레드를 1개로 제한하여 컨텍스트 부하 및 메모리 크래시 방지
        if ("test".equalsIgnoreCase(activeProfile)) {
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
        } else {
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(8);
        }
        
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("CR-Worker-");
        executor.initialize();
        return executor;
    }

    /**
     * [ColumnRangePartitioner] 테이블 데이터 분할기
     * "1억 건의 데이터를 4개로 쪼개줘!"라고 요청하면 ID 범위를 계산하여 구역을 나눕니다.
     * @StepScope: 스레드별로 동적인 파라미터를 받기 위해 사용합니다.
     */
    @Bean
    @StepScope
    public ColumnRangePartitioner partitioner(
            @Value("#{jobParameters['table'] ?: 'cr_accounts'}") String table,
            @Value("#{jobParameters['column'] ?: 'id'}") String column) {
        return new ColumnRangePartitioner(jdbcOperations, table, column);
    }

    /**
     * [QuerydslPagingItemReader] 계좌 조회용 타입 세이프 리더
     * QueryDSL을 사용하여 오타 없는 안전한 쿼리로 데이터를 200건씩 끊어서 읽어옵니다.
     * 파티셔닝에 의해 결정된 minValue, maxValue 범위의 데이터만 조회합니다.
     */
    @Bean
    @StepScope
    public QuerydslPagingItemReader<CrAccount> pagingAccountReader(
            @Value("#{stepExecutionContext['minValue']}") Long minValue,
            @Value("#{stepExecutionContext['maxValue']}") Long maxValue) {

        long resolvedMin = minValue == null ? 0L : minValue;
        long resolvedMax = maxValue == null ? -1L : maxValue;

        return new QuerydslPagingItemReader<>(
                entityManagerFactory,
                200, // Page Size: 메모리 효율을 위해 200건씩 끊어서 읽기
                queryFactory -> CrBatchQueryProvider.accountPagingQuery().apply(
                        queryFactory, new LongRange(resolvedMin, resolvedMax))
        );
    }

    /**
     * [QuerydslPagingItemReader] 대손충당금(IFRS9) 산출 결과 조회용 리더
     * 이미 생성된 산출 결과 레코드(CrRiskResult)를 읽어와서 후속 연산(EAD/LGD/RWA)을 수행할 때 사용합니다.
     */
    @Bean
    @StepScope
    public QuerydslPagingItemReader<CrRiskResult> pagingResultReader(
            @Value("#{stepExecutionContext['minValue']}") Long minValue,
            @Value("#{stepExecutionContext['maxValue']}") Long maxValue) {

        long resolvedMin = minValue == null ? 0L : minValue;
        long resolvedMax = maxValue == null ? -1L : maxValue;

        return new QuerydslPagingItemReader<>(
                entityManagerFactory,
                200,
                queryFactory -> CrBatchQueryProvider.resultPagingQuery().apply(
                        queryFactory, new CrBatchQueryProvider.LongRange(resolvedMin, resolvedMax))
        );
    }

    /**
     * [QuerydslPagingItemReader] 고객(Customer) 조회용 리더
     * 담보 배분 최적화(CRM) 시 고객 단위로 파티셔닝하여 병렬 처리할 때 사용합니다.
     */
    @Bean
    @StepScope
    public QuerydslPagingItemReader<CrCustomer> pagingCustomerReader(
            @Value("#{stepExecutionContext['minValue']}") Long minValue,
            @Value("#{stepExecutionContext['maxValue']}") Long maxValue) {

        long resolvedMin = minValue == null ? 0L : minValue;
        long resolvedMax = maxValue == null ? -1L : maxValue;

        return new QuerydslPagingItemReader<>(
                entityManagerFactory,
                100, // 고객은 100명씩 끊어서 처리 (Simplex 연산 부하 고려)
                queryFactory -> CrBatchQueryProvider.customerPagingQuery().apply(
                        queryFactory, new LongRange(resolvedMin, resolvedMax))
        );
    }
}
