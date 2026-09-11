package com.ho.account.ecl.batch.config;

import com.ho.account.ecl.batch.job.tasklet.AllowanceSummaryTasklet;
import com.ho.account.ecl.batch.processor.EadCrmProcessor;
import com.ho.account.ecl.batch.processor.EclProcessor;
import com.ho.account.ecl.batch.processor.StagingProcessor;
import com.ho.account.ecl.batch.support.CacheWarmingTasklet;
import com.ho.account.ecl.batch.support.ColumnRangePartitioner;
import com.ho.account.ecl.batch.support.QuerydslPagingItemReader;
import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
import com.ho.account.ecl.core.application.service.allowance.AllowanceCalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.partition.support.PartitionStep;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BatchGridResourceTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AllowanceStagingBatchConfig.class, ExposureLgdBatchConfig.class,
                    MainReportingBatchConfig.class)
            .withBean(JobRepository.class, () -> mock(JobRepository.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(AllowanceCalculationService.class, () -> mock(AllowanceCalculationService.class))
            .withBean(StagingProcessor.class, () -> mock(StagingProcessor.class))
            .withBean(EadCrmProcessor.class, () -> mock(EadCrmProcessor.class))
            .withBean(EclProcessor.class, () -> mock(EclProcessor.class))
            .withBean(AllowanceEclResultRepository.class, () -> mock(AllowanceEclResultRepository.class))
            .withBean(CacheWarmingTasklet.class, () -> mock(CacheWarmingTasklet.class))
            .withBean(AllowanceSummaryTasklet.class, () -> mock(AllowanceSummaryTasklet.class))
            .withBean(TaskExecutor.class, SyncTaskExecutor::new)
            .withBean("accountPartitioner", ColumnRangePartitioner.class, () -> mock(ColumnRangePartitioner.class))
            .withBean("resultPartitioner", ColumnRangePartitioner.class, () -> mock(ColumnRangePartitioner.class))
            .withBean(QuerydslPagingItemReader.class, () -> mock(QuerydslPagingItemReader.class));

    @Test
    void retainsFourPartitionsWithoutAnExplicitLimit() {
        assertGridSize(contextRunner, 4);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void bindsAnExplicitLimitToEveryManagerStep(int gridSize) {
        assertGridSize(contextRunner.withPropertyValues("account.ecl.batch.grid-size=" + gridSize), gridSize);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonpositiveGridSizeBeforeStartingAJob(int gridSize) {
        contextRunner.withPropertyValues("account.ecl.batch.grid-size=" + gridSize).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("account.ecl.batch.grid-size must be positive");
        });
    }

    private void assertGridSize(ApplicationContextRunner runner, int expectedSize) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            for (String stepName : new String[]{"stagingManagerStep", "eadCrmManagerStep", "eclManagerStep"}) {
                PartitionStep step = context.getBean(stepName, PartitionStep.class);
                // Check the built handler, so a property that never reaches a manager step fails this test.
                TaskExecutorPartitionHandler handler = (TaskExecutorPartitionHandler)
                        ReflectionTestUtils.getField(step, "partitionHandler");
                assertThat(handler).isNotNull();
                assertThat(handler.getGridSize()).as(stepName).isEqualTo(expectedSize);
            }
        });
    }
}
