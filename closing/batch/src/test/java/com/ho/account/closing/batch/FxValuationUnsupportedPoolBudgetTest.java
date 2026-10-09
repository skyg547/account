package com.ho.account.closing.batch;

import com.ho.account.closing.application.port.in.FinancialClosingCalculation;
import com.ho.account.closing.batch.adapter.out.JournalFxValuationBalanceSource;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(FxValuationSourcePoolConcurrencyTest.Fixture.class)
@TestPropertySource(properties = {
        "account.closing.batch.fx.grid-size=3",
        "closing.sources.journal.maximum-pool-size=2"
})
class FxValuationUnsupportedPoolBudgetTest {
    @Autowired JobLauncher launcher;
    @Autowired Job fxValuationJob;
    @Autowired JournalFxValuationBalanceSource balanceSource;
    @Autowired FinancialClosingCalculation financialClosingCalculation;

    @Test
    void rejectsOversubscribedGridBeforeEvidenceScanOrPartitionCreation() {
        var parameters = new JobParametersBuilder()
                .addString("valuationDate", LocalDate.of(2026, 9, 14).toString())
                .addLong("valuationBatchId", 894L).toJobParameters();

        assertThatThrownBy(() -> launcher.run(fxValuationJob, parameters))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("grid-size");
        verifyNoInteractions(balanceSource, financialClosingCalculation);
    }
}
