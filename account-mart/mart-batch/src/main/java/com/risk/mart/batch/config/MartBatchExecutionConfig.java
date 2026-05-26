package com.risk.mart.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * mart-batch 공통 실행 설정.
 * 대량 적재/가공 단계에서 재사용할 Chunk 크기와 TaskExecutor를 제공합니다.
 */
@Configuration
public class MartBatchExecutionConfig {

    public static final int DEFAULT_CHUNK_SIZE = 1000;
    public static final int DQ_CHUNK_SIZE = 500;
    public static final int DEFAULT_THROTTLE_LIMIT = 8;

    @Bean(name = "martBatchTaskExecutor")
    public TaskExecutor martBatchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(DEFAULT_THROTTLE_LIMIT);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("mart-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
