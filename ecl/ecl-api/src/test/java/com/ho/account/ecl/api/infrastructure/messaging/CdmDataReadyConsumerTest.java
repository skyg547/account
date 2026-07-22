package com.ho.account.ecl.api.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.shared.finance.event.CdmDataReadyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;

class CdmDataReadyConsumerTest {

    @Test
    void usesEventIdentityAsSpringBatchJobIdentity() throws Exception {
        JobLauncher jobLauncher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        when(jobLauncher.run(eq(job), any(JobParameters.class)))
                .thenReturn(mock(JobExecution.class));
        CdmDataReadyConsumer consumer = new CdmDataReadyConsumer(jobLauncher, job);
        CdmDataReadyEvent event = event();

        consumer.handleCdmDataReady(event);

        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(job), parameters.capture());
        assertThat(parameters.getValue().getString("eventId")).isEqualTo("event-1");
        assertThat(parameters.getValue().getString("baseDate")).isEqualTo("2026-07-22");
        assertThat(parameters.getValue().getString("traceId")).isEqualTo("trace-1");
        assertThat(parameters.getValue().getParameters()).doesNotContainKey("timestamp");
    }

    @Test
    void treatsAnAlreadyCompletedEventAsAnIdempotentDuplicate() throws Exception {
        JobLauncher jobLauncher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        when(jobLauncher.run(eq(job), any(JobParameters.class)))
                .thenThrow(new JobInstanceAlreadyCompleteException("already complete"));
        CdmDataReadyConsumer consumer = new CdmDataReadyConsumer(jobLauncher, job);

        assertThatCode(() -> consumer.handleCdmDataReady(event())).doesNotThrowAnyException();
    }

    @Test
    void propagatesJobFailureSoKafkaCanApplyRetryPolicy() throws Exception {
        JobLauncher jobLauncher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        when(jobLauncher.run(eq(job), any(JobParameters.class)))
                .thenThrow(new JobExecutionException("job failed"));
        CdmDataReadyConsumer consumer = new CdmDataReadyConsumer(jobLauncher, job);

        assertThatThrownBy(() -> consumer.handleCdmDataReady(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event-1")
                .hasCauseInstanceOf(JobExecutionException.class);
    }

    private CdmDataReadyEvent event() {
        return new CdmDataReadyEvent(
                "event-1",
                LocalDate.of(2026, 7, 22),
                "trace-1",
                LocalDateTime.of(2026, 7, 22, 18, 0));
    }
}