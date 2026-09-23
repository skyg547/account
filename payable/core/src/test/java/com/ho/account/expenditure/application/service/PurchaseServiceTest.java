package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.in.PurchaseInvoiceCommand;
import com.ho.account.expenditure.application.port.out.PayableAccountMappingPort;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseInvoicePersistencePort purchaseInvoicePersistencePort;
    @Mock
    private PayablePersistencePort payablePersistencePort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private PayableAccountMappingPort payableAccountMappingPort;
    @Mock
    private AccountingPeriodStatusPort accountingPeriodStatusPort;

    private PurchaseService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseService(
                purchaseInvoicePersistencePort,
                payablePersistencePort,
                masterDataQueryPort,
                journalPostingPort,
                payableAccountMappingPort,
                accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("매입 인보이스 생성 시 actor와 계정 매핑 정책을 전표에 반영한다")
    void createPurchaseInvoiceUsesActorAndMappedAccounts() {
        PurchaseInvoiceCommand command = createCommand(" buyer-user ");
        when(accountingPeriodStatusPort.isClosed(command.issueDate())).thenReturn(false);
        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode("PI-001", "V001")).thenReturn(Optional.empty());
        when(purchaseInvoicePersistencePort.save(any(PurchaseInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payablePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payableAccountMappingPort.resolvePurchaseRecognitionAccounts(any(PurchaseInvoice.class)))
                .thenReturn(new PayableAccountMappingPort.PurchaseRecognitionAccounts("EXP-001", "VAT-001", "AP-001"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(10L, "SLIP-1", "DRAFT"));

        PurchaseInvoice saved = service.createPurchaseInvoice(command);

        assertThat(saved.getCreatedBy()).isEqualTo("buyer-user");

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());
        JournalEntryCommand journalCommand = commandCaptor.getValue();

        assertThat(journalCommand.createdBy()).isEqualTo("buyer-user");
        assertThat(journalCommand.auditUser()).isEqualTo("buyer-user");
        assertThat(journalCommand.lines()).extracting("accountCode")
                .containsExactly("EXP-001", "VAT-001", "AP-001");
    }

    @Test
    @DisplayName("매입 인보이스 생성 actor가 없으면 저장 전에 실패한다")
    void createPurchaseInvoiceRequiresActor() {
        assertThatThrownBy(() -> service.createPurchaseInvoice(createCommand(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("createdBy is required");
    }

    @Test
    @DisplayName("마감된 회계기간에 대한 매입 인보이스 생성 시 IllegalStateException이 발생한다")
    void createPurchaseInvoiceThrowsExceptionWhenAccountingPeriodClosed() {
        PurchaseInvoiceCommand command = createCommand("buyer-user");
        when(accountingPeriodStatusPort.isClosed(command.issueDate())).thenReturn(true);

        assertThatThrownBy(() -> service.createPurchaseInvoice(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간입니다");
    }

    @Test
    @DisplayName("차변과 대변 합계가 일치하지 않는 전표 검증 시 IllegalArgumentException이 발생한다")
    void validateJournalBalanceThrowsExceptionWhenImbalanced() {
        JournalEntryCommand imbalancedCommand = new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Imbalanced Purchase Entry",
                "TEST",
                null, null, "user", "user",
                "TEST", "1",
                java.util.List.of(
                        new com.ho.account.contracts.journal.JournalLineCommand("DEBIT", "50100", new BigDecimal("1000.00"), null, null, "V001", "Expense"),
                        new com.ho.account.contracts.journal.JournalLineCommand("CREDIT", "20100", new BigDecimal("900.00"), null, null, "V001", "AP")));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service, "validateJournalBalance", imbalancedCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차변 합계(1000.00)와 대변 합계(900.00)가 일치하지 않습니다");
    }

    @Test
    @DisplayName("상태 조건이 없으면 모든 매입 인보이스를 조회한다")
    void findInvoicesWithoutStatusFindsAllInvoices() {
        PurchaseInvoice invoice = invoice(1L, PurchaseInvoiceStatus.RECEIVED);
        when(purchaseInvoicePersistencePort.findAll()).thenReturn(List.of(invoice));

        assertThat(service.findInvoices(null)).containsExactly(invoice);

        verify(purchaseInvoicePersistencePort).findAll();
    }

    @Test
    @DisplayName("빈 상태 조건은 전체 조회로 처리한다")
    void findInvoicesWithBlankStatusFindsAllInvoices() {
        when(purchaseInvoicePersistencePort.findAll()).thenReturn(List.of());

        assertThat(service.findInvoices("  ")).isEmpty();

        verify(purchaseInvoicePersistencePort).findAll();
    }

    @Test
    @DisplayName("상태 조건을 정규화해 해당 매입 인보이스를 조회한다")
    void findInvoicesWithStatusFindsMatchingInvoices() {
        PurchaseInvoice invoice = invoice(2L, PurchaseInvoiceStatus.OVERDUE);
        when(purchaseInvoicePersistencePort.findByStatus(PurchaseInvoiceStatus.OVERDUE))
                .thenReturn(List.of(invoice));

        assertThat(service.findInvoices(" overdue ")).containsExactly(invoice);

        verify(purchaseInvoicePersistencePort).findByStatus(PurchaseInvoiceStatus.OVERDUE);
    }

    @Test
    @DisplayName("알 수 없는 상태 조건은 영속성 조회 전에 거부한다")
    void findInvoicesRejectsUnknownStatusBeforePersistenceQuery() {
        assertThatThrownBy(() -> service.findInvoices("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid purchase invoice status: unknown");

        verifyNoInteractions(purchaseInvoicePersistencePort);
    }

    @Test
    @DisplayName("식별자로 매입 인보이스를 조회한다")
    void findInvoiceByIdReturnsInvoice() {
        PurchaseInvoice invoice = invoice(3L, PurchaseInvoiceStatus.RECEIVED);
        when(purchaseInvoicePersistencePort.findById(3L)).thenReturn(Optional.of(invoice));

        assertThat(service.findInvoiceById(3L)).isSameAs(invoice);

        verify(purchaseInvoicePersistencePort).findById(3L);
    }

    @Test
    @DisplayName("매입 인보이스 식별자가 없으면 조회에 실패한다")
    void findInvoiceByIdThrowsWhenInvoiceDoesNotExist() {
        when(purchaseInvoicePersistencePort.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findInvoiceById(404L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("PurchaseInvoice not found with id: 404");
    }

    private PurchaseInvoiceCommand createCommand(String createdBy) {
        return new PurchaseInvoiceCommand(
                "PI-001",
                "V001",
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("1100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1000.00"),
                createdBy,
                null);
    }

    private PurchaseInvoice invoice(Long id, PurchaseInvoiceStatus status) {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setId(id);
        invoice.setStatus(status);
        return invoice;
    }

}
