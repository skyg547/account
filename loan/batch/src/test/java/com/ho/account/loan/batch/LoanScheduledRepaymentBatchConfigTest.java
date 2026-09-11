package com.ho.account.loan.batch;

import com.ho.account.loan.batch.config.LoanScheduledRepaymentBatchConfig;
import com.ho.account.loan.service.ScheduledRepaymentService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoanScheduledRepaymentBatchConfigTest {
    private final ScheduledRepaymentService service = mock(ScheduledRepaymentService.class);
    private final LoanScheduledRepaymentBatchConfig configuration = new LoanScheduledRepaymentBatchConfig(
            mock(JobRepository.class), mock(PlatformTransactionManager.class),
            mock(EntityManagerFactory.class), service);

    @Test
    void stableDateIsRequiredAndMustIdentifyTheJob() {
        var validator = configuration.loanScheduledRepaymentParametersValidator();
        assertThatCode(() -> validator.validate(new JobParametersBuilder()
                .addString("repaymentDate", "2026-09-11").toJobParameters())).doesNotThrowAnyException();
        for (var parameters : new org.springframework.batch.core.JobParameters[]{
                new JobParametersBuilder().toJobParameters(),
                new JobParametersBuilder().addString("repaymentDate", "yesterday").toJobParameters(),
                new JobParametersBuilder().addString("repaymentDate", "2026-09-11", false).toJobParameters()
        }) {
            assertThatThrownBy(() -> validator.validate(parameters))
                    .isInstanceOf(JobParametersInvalidException.class);
        }
    }

    @Test
    void pendingRemoteWriteFailsTheChunkAndStopsFollowingItems() {
        LocalDate date = LocalDate.of(2026, 9, 11);
        when(service.processIndividualRepayment(1L, date))
                .thenReturn(ScheduledRepaymentService.RepaymentResult.ALREADY_SUCCESSFUL);
        when(service.processIndividualRepayment(2L, date))
                .thenThrow(new IllegalStateException("Pending reservation requires reconciliation"));
        var writer = configuration.scheduledRepaymentWriter(date.toString());

        assertThatThrownBy(() -> writer.write(new Chunk<>(1L, 2L, 3L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Pending");
        verify(service).processIndividualRepayment(1L, date);
        verify(service, never()).processIndividualRepayment(3L, date);
    }
}
