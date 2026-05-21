package com.ho.account.reporting.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.in.SubmitRegulatoryReportUseCase;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import com.ho.account.reporting.domain.model.ReportLine;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReportingControllerTest {

    @Mock
    private GenerateStatementUseCase generateStatementUseCase;

    @Mock
    private ExportStatementDocumentUseCase exportStatementDocumentUseCase;

    @Mock
    private SubmitRegulatoryReportUseCase submitRegulatoryReportUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportingController controller = new ReportingController(
                generateStatementUseCase,
                exportStatementDocumentUseCase,
                submitRegulatoryReportUseCase);
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

    @Test
    void generateStatementDocument_returnsAttachmentAndDelegatesToUseCase() throws Exception {
        byte[] payload = "lineCode,label\nASSET_CASH,Cash\n".getBytes(StandardCharsets.UTF_8);
        ExportStatementDocumentUseCase.ExportedDocument document =
                new ExportStatementDocumentUseCase.ExportedDocument(
                        "balance_sheet_2026-03-31.csv",
                        "text/csv; charset=UTF-8",
                        payload);
        when(exportStatementDocumentUseCase.export(any())).thenReturn(document);

        mockMvc.perform(post("/api/v1/reporting/generate/document")
                        .param("type", "BALANCE_SHEET")
                        .param("baseDate", "2026-03-31T00:00:00")
                        .param("format", "EXCEL")
                        .header("X-User-ID", "tester"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename=\"balance_sheet_2026-03-31.csv\"")))
                .andExpect(content().bytes(payload));

        ArgumentCaptor<ExportStatementDocumentUseCase.ExportCommand> captor =
                ArgumentCaptor.forClass(ExportStatementDocumentUseCase.ExportCommand.class);
        verify(exportStatementDocumentUseCase).export(captor.capture());

        ExportStatementDocumentUseCase.ExportCommand command = captor.getValue();
        Assertions.assertThat(command.type())
                .isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        Assertions.assertThat(command.baseDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 0, 0));
        Assertions.assertThat(command.requesterId()).isEqualTo("tester");
        Assertions.assertThat(command.format())
                .isEqualTo(ExportStatementDocumentUseCase.DocumentFormat.EXCEL);
    }

    @Test
    void submitRegulatoryReport_delegatesToUseCase_withCorrectionReason() throws Exception {
        FinancialStatement statement = finalizedBalanceSheet("ST-001");
        RegulatoryReportSubmission submission = RegulatoryReportSubmission.ready(
                "SUB-001",
                statement,
                2,
                "tester",
                "Correct prior submission",
                LocalDateTime.of(2026, 4, 1, 9, 0));
        when(submitRegulatoryReportUseCase.submit(any())).thenReturn(submission);

        mockMvc.perform(post("/api/v1/reporting/submissions/regulatory")
                        .param("type", "BALANCE_SHEET")
                        .param("baseDate", "2026-03-31T00:00:00")
                        .param("correctionReason", "Correct prior submission")
                        .header("X-User-ID", "tester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value("SUB-001"))
                .andExpect(jsonPath("$.statementId").value("ST-001"))
                .andExpect(jsonPath("$.statementType").value("BALANCE_SHEET"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("READY"));

        ArgumentCaptor<SubmitRegulatoryReportUseCase.SubmitCommand> captor =
                ArgumentCaptor.forClass(SubmitRegulatoryReportUseCase.SubmitCommand.class);
        verify(submitRegulatoryReportUseCase).submit(captor.capture());

        SubmitRegulatoryReportUseCase.SubmitCommand command = captor.getValue();
        Assertions.assertThat(command.type())
                .isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        Assertions.assertThat(command.baseDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 0, 0));
        Assertions.assertThat(command.requesterId()).isEqualTo("tester");
        Assertions.assertThat(command.correctionReason()).isEqualTo("Correct prior submission");
    }

    private FinancialStatement finalizedBalanceSheet(String statementId) {
        FinancialStatement statement = new FinancialStatement(
                statementId,
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
        return statement;
    }
}
