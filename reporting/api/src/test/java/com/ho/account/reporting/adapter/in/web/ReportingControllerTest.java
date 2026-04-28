package com.ho.account.reporting.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReportingControllerTest {

    @Mock
    private GenerateStatementUseCase generateStatementUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportingController controller = new ReportingController(generateStatementUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void generateStatement_delegatesToUseCase_withRequestValues() throws Exception {
        FinancialStatement statement = new FinancialStatement(
                "ST-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0));
        statement.addLine(new ReportLine(
                "ASSET_CASH",
                "Cash",
                new BigDecimal("850000000"),
                new BigDecimal("700000000"),
                "3",
                1));
        statement.finalizeStatement();
        when(generateStatementUseCase.generate(any())).thenReturn(statement);

        mockMvc.perform(post("/api/v1/reporting/generate")
                        .param("type", "BALANCE_SHEET")
                        .param("baseDate", "2026-03-31T00:00:00")
                        .header("X-User-ID", "tester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value("ST-001"))
                .andExpect(jsonPath("$.type").value("BALANCE_SHEET"));

        ArgumentCaptor<GenerateStatementUseCase.GenerateCommand> captor =
                ArgumentCaptor.forClass(GenerateStatementUseCase.GenerateCommand.class);
        verify(generateStatementUseCase).generate(captor.capture());

        GenerateStatementUseCase.GenerateCommand command = captor.getValue();
        Assertions.assertThat(command.type())
                .isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        Assertions.assertThat(command.baseDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 0, 0));
        Assertions.assertThat(command.requesterId()).isEqualTo("tester");
    }
}
