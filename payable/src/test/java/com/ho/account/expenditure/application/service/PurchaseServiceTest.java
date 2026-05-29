package com.ho.account.expenditure.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.out.PayableAccountMappingPort;
import com.ho.account.expenditure.application.port.out.PayablePersistencePort;
import com.ho.account.expenditure.application.port.out.PurchaseInvoicePersistencePort;
import com.ho.account.expenditure.domain.PurchaseInvoice;
import com.ho.account.expenditure.domain.PurchaseInvoiceStatus;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseInvoicePersistencePort purchaseInvoicePersistencePort;
    @Mock
    private PayablePersistencePort payablePersistencePort;
    @Mock
    private BusinessPartnerPersistencePort businessPartnerPersistencePort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private PayableAccountMappingPort payableAccountMappingPort;

    private PurchaseService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseService(
                purchaseInvoicePersistencePort,
                payablePersistencePort,
                businessPartnerPersistencePort,
                masterDataQueryPort,
                journalPostingPort,
                payableAccountMappingPort);
    }

    @Test
    @DisplayName("매입 인보이스 생성 시 actor와 계정 매핑 정책을 전표에 반영한다")
    void createPurchaseInvoiceUsesActorAndMappedAccounts() {
        PurchaseInvoice invoice = createInvoice();
        invoice.setCreatedBy(" buyer-user ");
        BusinessPartner vendor = vendor();

        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(businessPartnerPersistencePort.findByBusinessPartnerCode("V001")).thenReturn(Optional.of(vendor));
        when(purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode("PI-001", "V001")).thenReturn(Optional.empty());
        when(purchaseInvoicePersistencePort.save(any(PurchaseInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payablePersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payableAccountMappingPort.resolvePurchaseRecognitionAccounts(invoice))
                .thenReturn(new PayableAccountMappingPort.PurchaseRecognitionAccounts("EXP-001", "VAT-001", "AP-001"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(10L, "SLIP-1", "DRAFT"));

        PurchaseInvoice saved = service.createPurchaseInvoice(invoice);

        assertThat(saved.getCreatedBy()).isEqualTo("buyer-user");

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());
        JournalEntryCommand command = commandCaptor.getValue();

        assertThat(command.createdBy()).isEqualTo("buyer-user");
        assertThat(command.auditUser()).isEqualTo("buyer-user");
        assertThat(command.lines()).extracting("accountCode")
                .containsExactly("EXP-001", "VAT-001", "AP-001");
    }

    @Test
    @DisplayName("매입 인보이스 생성 actor가 없으면 저장 전에 실패한다")
    void createPurchaseInvoiceRequiresActor() {
        PurchaseInvoice invoice = createInvoice();
        invoice.setCreatedBy(" ");

        when(masterDataQueryPort.findBusinessPartner("V001"))
                .thenReturn(Optional.of(new BusinessPartnerRef("V001", "Vendor One", "VENDOR", true)));
        when(businessPartnerPersistencePort.findByBusinessPartnerCode("V001")).thenReturn(Optional.of(vendor()));
        when(purchaseInvoicePersistencePort.findByInvoiceNoAndVendorCode("PI-001", "V001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPurchaseInvoice(invoice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("createdBy is required");
    }

    private PurchaseInvoice createInvoice() {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setInvoiceNo("PI-001");
        invoice.setVendorCode("V001");
        invoice.setIssueDate(LocalDate.of(2026, 5, 29));
        invoice.setDueDate(LocalDate.of(2026, 6, 30));
        invoice.setNetAmount(new BigDecimal("1000.00"));
        invoice.setTaxAmount(new BigDecimal("100.00"));
        invoice.setTotalAmount(new BigDecimal("1100.00"));
        invoice.setStatus(PurchaseInvoiceStatus.RECEIVED);
        return invoice;
    }

    private BusinessPartner vendor() {
        BusinessPartner vendor = new BusinessPartner();
        vendor.setBusinessPartnerCode("V001");
        vendor.setBusinessPartnerName("Vendor One");
        return vendor;
    }
}
