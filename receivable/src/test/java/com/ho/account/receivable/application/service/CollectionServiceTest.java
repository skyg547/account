package com.ho.account.receivable.application.service;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.application.port.out.ReceivableAccountMappingPort;
import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CollectionPersistencePort collectionPersistencePort;
    @Mock
    private ReceivablePersistencePort receivablePersistencePort;
    @Mock
    private SalesInvoicePersistencePort salesInvoicePersistencePort;
    @Mock
    private MasterDataQueryPort masterDataQueryPort;
    @Mock
    private JournalPostingPort journalPostingPort;
    @Mock
    private ReceivableAccountMappingPort receivableAccountMappingPort;

    private CollectionService service;

    @BeforeEach
    void setUp() {
        service = new CollectionService(
                collectionPersistencePort,
                receivablePersistencePort,
                salesInvoicePersistencePort,
                masterDataQueryPort,
                journalPostingPort,
                receivableAccountMappingPort);
    }

    @Test
    @DisplayName("수납 인식 전표 생성 시 수납 계정 매핑 정책을 반영한다")
    void receivePaymentUsesMappedAccounts() {
        Collection collection = collection();

        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer()));
        when(collectionPersistencePort.save(collection)).thenReturn(collection);
        when(receivableAccountMappingPort.resolveCollectionRecognitionAccounts(collection))
                .thenReturn(new ReceivableAccountMappingPort.CollectionRecognitionAccounts("CASH-001", "CLR-001"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(200L, "SLIP-200", "DRAFT"));

        service.receivePayment(collection);

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("CASH-001", "CLR-001");
    }

    @Test
    @DisplayName("수납 매칭 전표 생성 시 clearing/AR 계정 매핑 정책을 반영한다")
    void manualMatchCollectionUsesMappedAccounts() {
        Collection collection = collection();
        Receivable receivable = receivable();

        when(collectionPersistencePort.findById(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findById(30L)).thenReturn(Optional.of(receivable));
        when(receivablePersistencePort.save(receivable)).thenReturn(receivable);
        when(collectionPersistencePort.save(collection)).thenReturn(collection);
        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer()));
        when(receivableAccountMappingPort.resolveCollectionMatchAccounts(collection, receivable))
                .thenReturn(new ReceivableAccountMappingPort.CollectionMatchAccounts("CLR-002", "AR-002"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(300L, "SLIP-300", "DRAFT"));

        service.manualMatchCollection(20L, 30L, new BigDecimal("100.00"));

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("CLR-002", "AR-002");
    }

    private Collection collection() {
        Collection collection = new Collection();
        collection.setId(20L);
        collection.setCustomerCode("C001");
        collection.setCollectionDate(LocalDate.of(2026, 5, 29));
        collection.setAmount(new BigDecimal("100.00"));
        collection.setStatus(CollectionStatus.RECEIVED);
        return collection;
    }

    private Receivable receivable() {
        Receivable receivable = new Receivable();
        receivable.setId(30L);
        receivable.setCustomerCode("C001");
        receivable.setOriginalAmount(new BigDecimal("500.00"));
        receivable.setOutstandingAmount(new BigDecimal("500.00"));
        receivable.setDueDate(LocalDate.of(2026, 6, 30));
        receivable.setStatus(ReceivableStatus.OPEN);
        return receivable;
    }

    private BusinessPartnerRef customer() {
        return new BusinessPartnerRef("C001", "Customer One", "CUSTOMER", true);
    }
}
