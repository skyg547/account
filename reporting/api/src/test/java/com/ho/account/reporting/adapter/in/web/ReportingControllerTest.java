package com.ho.account.reporting.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.reporting.application.port.in.ExportStatementDocumentUseCase;
import com.ho.account.reporting.application.port.in.GenerateStatementUseCase;
import com.ho.account.reporting.application.port.in.DisclosureNoteMartUseCase;
import com.ho.account.reporting.application.port.in.SubmitRegulatoryFilingUseCase;
import com.ho.account.reporting.application.port.in.SubmitRegulatoryReportUseCase;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import com.ho.account.reporting.domain.model.RegulatoryFilingLine;
import com.ho.account.reporting.domain.model.RegulatoryFilingReceipt;
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

    @Mock
    private DisclosureNoteMartUseCase disclosureNoteMartUseCase;

    @Mock
    private SubmitRegulatoryFilingUseCase submitRegulatoryFilingUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReportingController controller = new ReportingController(
                generateStatementUseCase,
                exportStatementDocumentUseCase,
                submitRegulatoryReportUseCase,
                disclosureNoteMartUseCase,
                submitRegulatoryFilingUseCase);
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

    @Test
    void generateDisclosureNoteMart_delegatesToUseCase() throws Exception {
        DisclosureNoteMart mart = DisclosureNoteMart.fromStatement(
                finalizedBalanceSheet("ST-001"),
                "tester",
                LocalDateTime.of(2026, 4, 1, 9, 0));
        when(disclosureNoteMartUseCase.generate(any())).thenReturn(mart);

        mockMvc.perform(post("/api/v1/reporting/disclosure-notes/generate")
                        .param("type", "BALANCE_SHEET")
                        .param("baseDate", "2026-03-31T00:00:00")
                        .header("X-User-ID", "tester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value("ST-001"))
                .andExpect(jsonPath("$.statementType").value("BALANCE_SHEET"))
                .andExpect(jsonPath("$.entries[0].noteNumber").value("3"))
                .andExpect(jsonPath("$.entries[0].noteCategory").value("CURRENCY"));

        ArgumentCaptor<DisclosureNoteMartUseCase.GenerateCommand> captor =
                ArgumentCaptor.forClass(DisclosureNoteMartUseCase.GenerateCommand.class);
        verify(disclosureNoteMartUseCase).generate(captor.capture());

        DisclosureNoteMartUseCase.GenerateCommand command = captor.getValue();
        Assertions.assertThat(command.type())
                .isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        Assertions.assertThat(command.baseDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 0, 0));
        Assertions.assertThat(command.requesterId()).isEqualTo("tester");
    }

    @Test
    void findDisclosureNoteMart_returnsStoredMart() throws Exception {
        DisclosureNoteMart mart = DisclosureNoteMart.fromStatement(
                finalizedBalanceSheet("ST-001"),
                "tester",
                LocalDateTime.of(2026, 4, 1, 9, 0));
        when(disclosureNoteMartUseCase.find(any())).thenReturn(java.util.Optional.of(mart));

        mockMvc.perform(get("/api/v1/reporting/disclosure-notes")
                        .param("type", "BALANCE_SHEET")
                        .param("baseDate", "2026-03-31T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value("ST-001"))
                .andExpect(jsonPath("$.entries[0].sourceLineCode").value("ASSET_CASH"));

        ArgumentCaptor<DisclosureNoteMartUseCase.FindQuery> captor =
                ArgumentCaptor.forClass(DisclosureNoteMartUseCase.FindQuery.class);
        verify(disclosureNoteMartUseCase).find(captor.capture());

        DisclosureNoteMartUseCase.FindQuery query = captor.getValue();
        Assertions.assertThat(query.type())
                .isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        Assertions.assertThat(query.baseDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 0, 0));
    }

    @Test
    void submitRegulatoryFiling_delegatesToUseCase() throws Exception {
        RegulatoryFiling filing = regulatoryFiling();
        when(submitRegulatoryFilingUseCase.submit(any())).thenReturn(filing);

        mockMvc.perform(post("/api/v1/reporting/regulatory-filings/submit")
                        .param("type", "BALANCE_SHEET")
                        .param("baseDate", "2026-03-31T00:00:00")
                        .param("targetAgency", "FSS")
                        .header("X-User-ID", "tester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filingId").value("FILING-001"))
                .andExpect(jsonPath("$.submissionId").value("SUB-001"))
                .andExpect(jsonPath("$.targetAgency").value("FSS"))
                .andExpect(jsonPath("$.regulatorReceiptId").value("LOCAL-FSS-BALANCE_SHEET-20260331-V1"))
                .andExpect(jsonPath("$.lines[0].fieldCode").value("CASH_AND_CASH_EQUIVALENTS"));

        ArgumentCaptor<SubmitRegulatoryFilingUseCase.SubmitCommand> captor =
                ArgumentCaptor.forClass(SubmitRegulatoryFilingUseCase.SubmitCommand.class);
        verify(submitRegulatoryFilingUseCase).submit(captor.capture());

        SubmitRegulatoryFilingUseCase.SubmitCommand command = captor.getValue();
        Assertions.assertThat(command.type())
                .isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        Assertions.assertThat(command.baseDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 0, 0));
        Assertions.assertThat(command.requesterId()).isEqualTo("tester");
        Assertions.assertThat(command.targetAgency()).isEqualTo("FSS");
    }

    @Test
    void findLatestRegulatoryFiling_returnsStoredFiling() throws Exception {
        when(submitRegulatoryFilingUseCase.findLatest(any())).thenReturn(java.util.Optional.of(regulatoryFiling()));

        mockMvc.perform(get("/api/v1/reporting/regulatory-filings/latest")
                        .param("type", "BALANCE_SHEET")
                        .param("baseDate", "2026-03-31T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filingId").value("FILING-001"))
                .andExpect(jsonPath("$.lines[0].reportCode").value("FSS_BS_DISCLOSURE"));

        ArgumentCaptor<SubmitRegulatoryFilingUseCase.FindLatestQuery> captor =
                ArgumentCaptor.forClass(SubmitRegulatoryFilingUseCase.FindLatestQuery.class);
        verify(submitRegulatoryFilingUseCase).findLatest(captor.capture());

        SubmitRegulatoryFilingUseCase.FindLatestQuery query = captor.getValue();
        Assertions.assertThat(query.type())
                .isEqualTo(FinancialStatement.StatementType.BALANCE_SHEET);
        Assertions.assertThat(query.baseDate())
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 0, 0));
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

    private RegulatoryFiling regulatoryFiling() {
        return RegulatoryFiling.restored(
                "FILING-001",
                "SUB-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0),
                1,
                "FSS",
                "tester",
                LocalDateTime.of(2026, 4, 1, 10, 0),
                RegulatoryFiling.FilingStatus.ACCEPTED,
                new RegulatoryFilingReceipt(
                        "LOCAL-FSS-BALANCE_SHEET-20260331-V1",
                        LocalDateTime.of(2026, 4, 1, 10, 0),
                        "Accepted").receiptId(),
                "Accepted",
                java.util.List.of(new RegulatoryFilingLine(
                        "FSS_BS_DISCLOSURE",
                        "CASH_AND_CASH_EQUIVALENTS",
                        "현금 및 현금성자산",
                        "3",
                        "ASSET_CASH",
                        "Cash",
                        new BigDecimal("850000000"),
                        new BigDecimal("700000000"),
                        10)));
    }
}
