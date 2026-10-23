package com.ho.account.journalledger.batch.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;

import java.lang.reflect.Method;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/** Byte-identical RED/GREEN proof that a restart reads the range frozen by the start step. */
class BatchDateRangeRestartRegressionTest {

    @Test
    @ResourceLock("default-time-zone")
    void restartKeepsTheNoDateRangeEvenWhenTheJvmCalendarDayChanges() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            JobExecution execution = new JobExecution(
                    new JobInstance(768L, "reaggregation"),
                    new JobParametersBuilder().toJobParameters());
            StepExecution start = new StepExecution("start", execution);

            TimeZone.setDefault(TimeZone.getTimeZone("GMT-12:00"));
            Object first = freezeWhenSupported(start);

            TimeZone.setDefault(TimeZone.getTimeZone("GMT+14:00"));
            Object restarted = readFrozenWhenSupported(new StepExecution("writer", execution));

            assertThat(restarted).isEqualTo(first);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private Object freezeWhenSupported(StepExecution stepExecution) throws Exception {
        try {
            Method freeze = BatchDateRangeParameterUtils.class
                    .getMethod("freezeDateRange", StepExecution.class);
            return freeze.invoke(null, stepExecution);
        } catch (NoSuchMethodException auditedImplementation) {
            return BatchDateRangeParameterUtils.resolveDateRange(stepExecution);
        }
    }

    private Object readFrozenWhenSupported(StepExecution stepExecution) throws Exception {
        try {
            Method read = BatchDateRangeParameterUtils.class
                    .getMethod("resolveFrozenDateRange", StepExecution.class);
            return read.invoke(null, stepExecution);
        } catch (NoSuchMethodException auditedImplementation) {
            return BatchDateRangeParameterUtils.resolveDateRange(stepExecution);
        }
    }
}
