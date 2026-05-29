package com.ho.account.receivable.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.out.ReceivableAccountMappingPort;
import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.SalesInvoice;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private SalesService service;

    @BeforeEach
    void setUp() {
        service = new SalesService(
                salesInvoicePersistencePort,
                receivablePersistencePort,
                masterDataQueryPort,
                journalPostingPort,
                receivableAccountMappingPort);
    }

    @Test
    @DisplayName("매출 인식 전표 생성 시 receivable 계정 매핑 정책을 반영한다")
    void createSalesInvoiceUsesMappedAccounts() {
        SalesInvoice invoice = createInvoice();
        BusinessPartnerRef customer = new BusinessPartnerRef("C001", "Customer One", "CUSTOMER", true);

        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer));
        when(salesInvoicePersistencePort.save(invoice)).thenAnswer(invocation -> {
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

        service.createSalesInvoice(invoice);

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("AR-001", "REV-001", "VAT-001");
    }

    private SalesInvoice createInvoice() {
        return SalesInvoice.create(
                "SI-001",
                "C001",
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                "sales-user");
    }
}
