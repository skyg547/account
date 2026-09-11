package com.ho.account.closing.batch;

import com.ho.account.closing.application.pipeline.FxValuationPipeline;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.FxValuationBalance;
import com.ho.account.closing.batch.adapter.out.JournalFxValuationBalanceSource;
import com.ho.account.closing.batch.config.FxValuationBatchConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Runs the real partitioned Job and its step-scoped JDBC reader, without provider services. */
@SpringJUnitConfig(FxValuationScopedReaderLifecycleTest.BatchFixture.class)
@TestPropertySource(properties = {
        "account.closing.batch.fx.chunk-size=1",
        "account.closing.batch.fx.grid-size=1"
})
class FxValuationScopedReaderLifecycleTest {
    @Autowired JobLauncher launcher;
    @Autowired Job fxValuationJob;
    @Autowired DataSource dataSource;
    @Autowired JournalFxValuationBalanceSource source;
    @Autowired FxValuationPipeline pipeline;

    @Test
    void partitionedStepOpensReadsCheckpointsAndClosesItsScopedJdbcCursor() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 11);
        ExecutionContext range = new ExecutionContext();
        range.putString("startAccountCode", "110001");
        range.putString("endAccountCode", "110002");
        when(source.createPartitions(date, "KRW", 1)).thenReturn(Map.of("fx-range-1", range));
        JdbcCursorItemReader<FxValuationBalance> reader = spy(
                new JdbcCursorItemReaderBuilder<FxValuationBalance>()
                        .name("scoped-fx-reader")
                        .dataSource(dataSource)
                        .sql("SELECT '110001' AS account_code UNION ALL SELECT '110002' AS account_code")
                        .rowMapper((row, index) -> new FxValuationBalance(
                                row.getString("account_code"), "USD", BigDecimal.ONE,
                                new BigDecimal("1300.00")))
                        .saveState(true)
                        .build());
        when(source.createReader(date, "KRW", "110001", "110002", 1)).thenReturn(reader);

        var execution = launcher.run(fxValuationJob, new JobParametersBuilder()
                .addString("valuationDate", date.toString())
                .addLong("valuationBatchId", 690L).toJobParameters());

        assertThat(execution.getStatus()).as("failures: %s", execution.getAllFailureExceptions())
                .isEqualTo(BatchStatus.COMPLETED);
        var worker = execution.getStepExecutions().stream()
                .filter(step -> step.getStepName().contains(":fx-range-1"))
                .findFirst().orElseThrow();
        assertThat(worker.getReadCount()).isEqualTo(2);
        assertThat(worker.getWriteCount()).isEqualTo(2);
        assertThat(worker.getExecutionContext().getInt("scoped-fx-reader.read.count")).isGreaterThanOrEqualTo(2);
        var lifecycle = inOrder(reader);
        lifecycle.verify(reader).open(any(ExecutionContext.class));
        lifecycle.verify(reader).read();
        lifecycle.verify(reader).update(any(ExecutionContext.class));
        lifecycle.verify(reader, atLeastOnce()).close();
        verify(pipeline, times(2)).processChunk(anyList(), eq(date), eq(690L));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableBatchProcessing
    @Import(FxValuationBatchConfig.class)
    static class BatchFixture {
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

        @Bean
        JournalFxValuationBalanceSource source() {
            return mock(JournalFxValuationBalanceSource.class);
        }

        @Bean
        FxValuationPipeline pipeline() {
            return mock(FxValuationPipeline.class);
        }

        @Bean
        ClosingAccountingProperties accountingProperties() {
            return new ClosingAccountingProperties();
        }
    }
}
