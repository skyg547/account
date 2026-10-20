package com.ho.account.closing.batch;

import com.ho.account.closing.application.pipeline.FxValuationPipeline;
import com.ho.account.closing.application.port.in.FinancialClosingCalculation;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.FxValuationBalance;
import com.ho.account.closing.batch.adapter.out.JournalFxValuationBalanceSource;
import com.ho.account.closing.batch.config.FxValuationBatchConfig;
import com.ho.account.closing.infrastructure.source.ClosingReadOnlySources;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Real Hikari acquisition through two concurrent step-scoped cursor readers. */
@SpringJUnitConfig(FxValuationSourcePoolConcurrencyTest.Fixture.class)
@TestPropertySource(properties = {
        "account.closing.batch.fx.chunk-size=1",
        "account.closing.batch.fx.grid-size=2",
        "closing.sources.journal.maximum-pool-size=2"
})
class FxValuationSourcePoolConcurrencyTest {
    @Autowired JobLauncher launcher;
    @Autowired Job fxValuationJob;
    @Autowired ClosingReadOnlySources sources;
    @Autowired JournalFxValuationBalanceSource balanceSource;

    @Test
    void twoPartitionsCompleteWhileFirstCursorOwnsConnectionLongerThanAcquisitionTimeout() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 13);
        CountDownLatch firstReading = new CountDownLatch(1);
        CountDownLatch secondOpening = new CountDownLatch(1);
        CountDownLatch secondReading = new CountDownLatch(1);
        Map<String, ExecutionContext> ranges = new LinkedHashMap<>();
        ranges.put("fx-range-1", range("110001"));
        ranges.put("fx-range-2", range("110002"));
        when(balanceSource.createPartitions(date, "KRW", 2)).thenReturn(ranges);
        when(balanceSource.createReader(date, "KRW", "110001", "110001", 1))
                .thenAnswer(invocation -> reader("110001", firstReading, secondOpening, secondReading, true));
        when(balanceSource.createReader(date, "KRW", "110002", "110002", 1))
                .thenAnswer(invocation -> reader("110002", firstReading, secondOpening, secondReading, false));

        var execution = launcher.run(fxValuationJob, new JobParametersBuilder()
                .addString("valuationDate", date.toString())
                .addLong("valuationBatchId", 893L).toJobParameters());

        assertThat(execution.getStatus()).as("failures: %s", execution.getAllFailureExceptions())
                .isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions().stream()
                .filter(step -> step.getStepName().contains(":fx-range-"))
                .mapToLong(step -> step.getReadCount()).sum()).isEqualTo(2);
        assertThat(((HikariDataSource) sources.journalDataSource()).getMaximumPoolSize()).isEqualTo(2);
    }

    private static ExecutionContext range(String accountCode) {
        ExecutionContext context = new ExecutionContext();
        context.putString("startAccountCode", accountCode);
        context.putString("endAccountCode", accountCode);
        return context;
    }

    private JdbcCursorItemReader<FxValuationBalance> reader(
            String accountCode, CountDownLatch firstReading,
            CountDownLatch secondOpening, CountDownLatch secondReading, boolean first) {
        JdbcCursorItemReader<FxValuationBalance> reader = new JdbcCursorItemReader<>() {
            @Override
            protected void doOpen() throws Exception {
                if (!first) {
                    if (!firstReading.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("first cursor never began reading");
                    }
                    secondOpening.countDown();
                }
                super.doOpen();
            }
        };
        reader.setName("fx-pool-" + accountCode);
        reader.setDataSource(sources.journalDataSource());
        reader.setSql("SELECT '" + accountCode + "' AS account_code");
        reader.setRowMapper((resultSet, rowNumber) -> {
            if (first) {
                // Prove the second reader obtains its own lease before this five-second hold ends.
                firstReading.countDown();
                try {
                    if (!secondOpening.await(10, TimeUnit.SECONDS)
                            || !secondReading.await(6, TimeUnit.SECONDS)) {
                        throw new java.sql.SQLException("second cursor did not acquire concurrently");
                    }
                    Thread.sleep(5_500);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new java.sql.SQLException("interrupted while holding cursor", exception);
                }
            } else {
                secondReading.countDown();
            }
            return new FxValuationBalance(resultSet.getString("account_code"), "USD",
                    BigDecimal.ONE, new BigDecimal("1300.00"));
        });
        reader.setFetchSize(1);
        return reader;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableBatchProcessing
    @Import(FxValuationBatchConfig.class)
    static class Fixture {
        @Bean(destroyMethod = "shutdown")
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("org/springframework/batch/core/schema-h2.sql").build();
        }

        @Bean
        JdbcTransactionManager transactionManager(DataSource dataSource) {
            return new JdbcTransactionManager(dataSource);
        }

        @Bean(destroyMethod = "close")
        ClosingReadOnlySources closingReadOnlySources() {
            var sources = new ClosingReadOnlySources(
                    "jdbc:postgresql://journal.invalid/journal", "reader", "test-only",
                    "jdbc:postgresql://ecl.invalid/ecl", "reader", "test-only",
                    "jdbc:postgresql://master.invalid/master", "reader", "test-only", 2);
            // Replace only the transport with H2; exercise the production Hikari pool and timeout.
            HikariDataSource journal = (HikariDataSource) sources.journalDataSource();
            journal.setJdbcUrl("jdbc:h2:mem:fx-source-pool;DB_CLOSE_DELAY=-1");
            journal.setConnectionInitSql(null);
            return sources;
        }

        @Bean JournalFxValuationBalanceSource balanceSource() {
            return mock(JournalFxValuationBalanceSource.class);
        }

        @Bean FxValuationPipeline pipeline() {
            return mock(FxValuationPipeline.class);
        }

        @Bean FinancialClosingCalculation financialClosingCalculation() {
            return mock(FinancialClosingCalculation.class);
        }

        @Bean ClosingAccountingProperties accountingProperties() {
            return new ClosingAccountingProperties();
        }
    }
}
