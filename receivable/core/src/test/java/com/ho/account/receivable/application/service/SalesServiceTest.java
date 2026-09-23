package com.ho.account.receivable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.in.SalesInvoiceCommand;
import com.ho.account.receivable.application.port.in.SalesUseCase.InvalidSalesInvoiceStatusException;
import com.ho.account.receivable.application.port.in.SalesUseCase.SalesInvoiceNotFoundException;
import com.ho.account.receivable.application.port.out.ReceivableAccountMappingPort;
import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private SalesInvoicePersistencePort salesInvoicePersistencePort;
    @Mock
    private ReceivablePersistencePort receivablePersistencePort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private ReceivableAccountMappingPort receivableAccountMappingPort;
    @Mock
    private AccountingPeriodStatusPort accountingPeriodStatusPort;

    private SalesService service;

    @BeforeEach
    void setUp() {
        service = new SalesService(
                salesInvoicePersistencePort,
                receivablePersistencePort,
                masterDataQueryPort,
                journalPostingPort,
                receivableAccountMappingPort,
                accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("매출 인식 전표 생성 시 receivable 계정 매핑 정책을 반영한다")
    void createSalesInvoiceUsesMappedAccounts() {
        SalesInvoiceCommand command = createCommand();
        BusinessPartnerRef customer = new BusinessPartnerRef("C001", "Customer One", "CUSTOMER", true);

        when(accountingPeriodStatusPort.isClosed(command.issueDate())).thenReturn(false);
        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer));
        when(salesInvoicePersistencePort.save(any(SalesInvoice.class))).thenAnswer(invocation -> {
            SalesInvoice saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
        when(receivablePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receivableAccountMappingPort.resolveSalesRecognitionAccounts(any(SalesInvoice.class)))
                .thenReturn(new ReceivableAccountMappingPort.SalesRecognitionAccounts("AR-001", "REV-001", "VAT-001"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(100L, "SLIP-100", "DRAFT"));

        service.createSalesInvoice(command);

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("AR-001", "REV-001", "VAT-001");
        assertThat(commandCaptor.getValue().createdBy()).isEqualTo("sales-user");
        assertThat(commandCaptor.getValue().auditUser()).isEqualTo("sales-user");
    }

    @Test
    @DisplayName("마감된 회계기간에 대한 매출 인보이스 생성 시 IllegalStateException이 발생한다")
    void createSalesInvoiceThrowsExceptionWhenAccountingPeriodClosed() {
        SalesInvoiceCommand command = createCommand();
        when(accountingPeriodStatusPort.isClosed(command.issueDate())).thenReturn(true);

        assertThatThrownBy(() -> service.createSalesInvoice(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간입니다");
    }

    @Test
    @DisplayName("차변과 대변 합계가 일치하지 않는 전표 검증 시 IllegalArgumentException이 발생한다")
    void validateJournalBalanceThrowsExceptionWhenImbalanced() {
        JournalEntryCommand imbalancedCommand = new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Imbalanced Sales Entry",
                "TEST",
                null, null, "user", "user",
                "TEST", "1",
                java.util.List.of(
                        new com.ho.account.contracts.journal.JournalLineCommand("DEBIT", "11100", new BigDecimal("1000.00"), null, null, "C001", "AR"),
                        new com.ho.account.contracts.journal.JournalLineCommand("CREDIT", "40100", new BigDecimal("950.00"), null, null, "C001", "Revenue")));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service, "validateJournalBalance", imbalancedCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차변 합계(1000.00)와 대변 합계(950.00)가 일치하지 않습니다");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("상태가 없거나 공백이면 전체 인보이스를 조회한다")
    void findInvoicesWithoutStatusReturnsAll(String status) {
        SalesInvoice invoice = createInvoice(10L, SalesInvoiceStatus.ISSUED);
        when(salesInvoicePersistencePort.findAll()).thenReturn(List.of(invoice));

        List<SalesInvoice> result = service.findInvoices(status);

        assertThat(result).containsExactly(invoice);
        verify(salesInvoicePersistencePort).findAll();
    }

    @Test
    @DisplayName("상태 문자열은 공백과 대소문자를 정규화한 enum으로 조회한다")
    void findInvoicesNormalizesStatusBeforeQuery() {
        SalesInvoice invoice = createInvoice(11L, SalesInvoiceStatus.PARTIAL_PAID);
        when(salesInvoicePersistencePort.findByStatus(SalesInvoiceStatus.PARTIAL_PAID))
                .thenReturn(List.of(invoice));

        List<SalesInvoice> result = service.findInvoices("  partial_paid ");

        assertThat(result).containsExactly(invoice);
        verify(salesInvoicePersistencePort).findByStatus(SalesInvoiceStatus.PARTIAL_PAID);
    }

    @Test
    @DisplayName("지원하지 않는 상태 문자열은 저장소 조회 전에 거부한다")
    void findInvoicesRejectsUnknownStatus() {
        assertThatThrownBy(() -> service.findInvoices("unknown"))
                .isInstanceOf(InvalidSalesInvoiceStatusException.class)
                .hasMessageContaining("Unknown sales invoice status: unknown");

        verifyNoInteractions(salesInvoicePersistencePort);
    }

    @Test
    @DisplayName("ID로 존재하는 인보이스를 조회한다")
    void findInvoiceByIdReturnsInvoice() {
        SalesInvoice invoice = createInvoice(12L, SalesInvoiceStatus.PAID);
        when(salesInvoicePersistencePort.findById(12L)).thenReturn(Optional.of(invoice));

        SalesInvoice result = service.findInvoiceById(12L);

        assertThat(result).isSameAs(invoice);
        verify(salesInvoicePersistencePort).findById(12L);
    }

    @Test
    @DisplayName("ID에 해당하는 인보이스가 없으면 명시적으로 실패한다")
    void findInvoiceByIdRejectsMissingInvoice() {
        when(salesInvoicePersistencePort.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findInvoiceById(404L))
                .isInstanceOf(SalesInvoiceNotFoundException.class)
                .hasMessageContaining("Sales invoice not found: 404");
    }

    private SalesInvoiceCommand createCommand() {
        return new SalesInvoiceCommand(
                "SI-001",
                "C001",
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("1100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1000.00"),
                "sales-user",
                null);
    }

    private SalesInvoice createInvoice(Long id, SalesInvoiceStatus status) {
        SalesInvoice invoice = SalesInvoice.create(
                "SI-QUERY",
                "C001",
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "sales-user");
        invoice.setId(id);
        invoice.setStatus(status);
        return invoice;
    }
}
