package com.ho.account.ecl.batch.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BatchWorkerResourceTest {
    private final BatchInfrastructureConfig configuration = new BatchInfrastructureConfig(
            mock(DataSource.class), mock(EntityManagerFactory.class), mock(JdbcOperations.class));

    @Test
    void preservesDefaultThroughputWithoutAnExplicitLimit() {
        assertPoolSizes("default", null, 4, 8);
        assertPoolSizes("external-dev", null, 4, 8);
    }

    @Test
    void preservesSingleWorkerTestProfileDefault() {
        assertPoolSizes("test", null, 1, 1);
    }

    @Test
    void executorHonorsLowResourceAndExplicitScaleSettings() {
        for (int workers : new int[]{1, 3}) {
            assertPoolSizes("external-dev", workers, workers, workers);
            assertPoolSizes("test", workers, workers, workers);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsAnExecutorWithoutWorkers(int workers) {
        assertThatThrownBy(() -> configuration.allowanceTaskExecutor("default", workers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("account.ecl.batch.worker-threads must be positive");
    }

    private void assertPoolSizes(String profile, Integer limit, int coreSize, int maxSize) {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) configuration.allowanceTaskExecutor(profile, limit);
        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(coreSize);
            assertThat(executor.getMaxPoolSize()).isEqualTo(maxSize);
        } finally {
            executor.shutdown();
        }
    }
}
