package com.ho.account.reporting.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase.GenerateCommand;
import com.ho.account.reporting.domain.model.FinancialStatement;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportingBatchAdapterTest {

    @Mock
    private GenerateStatementUseCase generateStatementUseCase;

    private ReportingBatchAdapter reportingBatchAdapter;

    @BeforeEach
    void setUp() {
        reportingBatchAdapter = new ReportingBatchAdapter(generateStatementUseCase);
    }

    @Test
    void runMonthlyClosingBatch_generatesBalanceSheetThenIncomeStatement_forMonthEndDate() {
        reportingBatchAdapter.runMonthlyClosingBatch(2026, 3);

        ArgumentCaptor<GenerateCommand> captor = ArgumentCaptor.forClass(GenerateCommand.class);
        verify(generateStatementUseCase, times(2)).generate(captor.capture());

        List<GenerateCommand> commands = captor.getAllValues();
        LocalDateTime expectedBaseDate = LocalDateTime.of(2026, 3, 31, 0, 0);

        assertThat(commands).hasSize(2);
        assertThat(commands.get(0).type()).isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        assertThat(commands.get(0).baseDate()).isEqualTo(expectedBaseDate);
        assertThat(commands.get(0).requesterId()).isEqualTo("SYSTEM_BATCH");

        assertThat(commands.get(1).type()).isEqualTo(FinancialStatement.StatementType.INCOME_STATEMENT);
        assertThat(commands.get(1).baseDate()).isEqualTo(expectedBaseDate);
        assertThat(commands.get(1).requesterId()).isEqualTo("SYSTEM_BATCH");
    }

    @Test
    void runStatementGenerationBatch_usesProvidedBaseDateAndRequester() {
        reportingBatchAdapter.runStatementGenerationBatch(java.time.LocalDate.of(2026, 6, 30), "batch-user");

        ArgumentCaptor<GenerateCommand> captor = ArgumentCaptor.forClass(GenerateCommand.class);
        verify(generateStatementUseCase, times(2)).generate(captor.capture());

        List<GenerateCommand> commands = captor.getAllValues();

        assertThat(commands.get(0).baseDate()).isEqualTo(LocalDateTime.of(2026, 6, 30, 0, 0));
        assertThat(commands.get(0).requesterId()).isEqualTo("batch-user");
        assertThat(commands.get(1).baseDate()).isEqualTo(LocalDateTime.of(2026, 6, 30, 0, 0));
        assertThat(commands.get(1).requesterId()).isEqualTo("batch-user");
    }
}
