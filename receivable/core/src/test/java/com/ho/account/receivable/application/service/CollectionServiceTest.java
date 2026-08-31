package com.ho.account.receivable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.receivable.application.port.in.CollectionCommand;
import com.ho.account.receivable.application.port.in.ManualMatchingCommand;
import com.ho.account.receivable.application.port.out.CollectionAllocationPersistencePort;
import com.ho.account.receivable.application.port.out.CollectionMatchingPolicyPort;
import com.ho.account.receivable.application.port.out.CollectionPersistencePort;
import com.ho.account.receivable.application.port.out.ReceivableAccountMappingPort;
import com.ho.account.receivable.application.port.out.ReceivablePersistencePort;
import com.ho.account.receivable.application.port.out.SalesInvoicePersistencePort;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionAllocation;
import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    @Mock
    private CollectionMatchingPolicyPort collectionMatchingPolicyPort;
    @Mock
    private CollectionAllocationPersistencePort collectionAllocationPersistencePort;
    @Mock
    private AccountingPeriodStatusPort accountingPeriodStatusPort;

    private CollectionService service;

    @BeforeEach
    void setUp() {
        service = new CollectionService(
                collectionPersistencePort,
                receivablePersistencePort,
                salesInvoicePersistencePort,
                masterDataQueryPort,
                journalPostingPort,
                receivableAccountMappingPort,
                collectionMatchingPolicyPort,
                collectionAllocationPersistencePort,
                accountingPeriodStatusPort);
    }

    @Test
    @DisplayName("수납 인식 전표 생성 시 수납 계정 매핑 정책을 반영한다")
    void receivePaymentUsesMappedAccounts() {
        CollectionCommand command = collectionCommand();
        when(accountingPeriodStatusPort.isClosed(command.collectionDate())).thenReturn(false);
        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer()));
        when(collectionPersistencePort.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        when(receivableAccountMappingPort.resolveCollectionRecognitionAccounts(any(Collection.class)))
                .thenReturn(new ReceivableAccountMappingPort.CollectionRecognitionAccounts("CASH-001", "CLR-001"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(200L, "SLIP-200", "DRAFT"));

        service.receivePayment(command);

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("CASH-001", "CLR-001");
    }

    @Test
    @DisplayName("동일한 referenceNo로 수납 요청 재시도 시 기존 수납을 반환하고 중복 저장 및 분개를 발생시키지 않는다")
    void receivePayment_withDuplicateReferenceNo_returnsExistingCollectionWithoutPostingJournal() {
        Collection existingCollection = collection();
        existingCollection.setReferenceNo("REF-DUPLICATE-001");
        CollectionCommand duplicateCommand = new CollectionCommand(
                LocalDate.of(2026, 5, 29),
                "C001",
                new BigDecimal("100.00"),
                "BANK-001",
                "VA-001",
                "REF-DUPLICATE-001");

        when(collectionPersistencePort.findByReferenceNo("REF-DUPLICATE-001"))
                .thenReturn(Optional.of(existingCollection));

        Collection result = service.receivePayment(duplicateCommand);

        assertThat(result).isSameAs(existingCollection);
        verify(collectionPersistencePort, never()).save(any());
        verify(journalPostingPort, never()).createDraftEntry(any());
        verify(masterDataQueryPort, never()).findBusinessPartner(any());
    }

    @Test
    @DisplayName("동일한 referenceNo로 두 번 호출 시 첫 번째만 영속화 및 전표를 발행하고 두 번째는 멱등하게 반환한다")
    void receivePayment_calledTwiceWithSameReferenceNo_persistsOnceAndPostsJournalOnce() {
        CollectionCommand command = new CollectionCommand(
                LocalDate.of(2026, 5, 29),
                "C001",
                new BigDecimal("100.00"),
                "BANK-001",
                "VA-001",
                "REF-IDEMPOTENT-002");

        Collection savedCollection = collection();
        savedCollection.setReferenceNo("REF-IDEMPOTENT-002");

        when(collectionPersistencePort.findByReferenceNo("REF-IDEMPOTENT-002"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedCollection));
        when(accountingPeriodStatusPort.isClosed(command.collectionDate())).thenReturn(false);
        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer()));
        when(collectionPersistencePort.save(any(Collection.class))).thenReturn(savedCollection);
        when(receivableAccountMappingPort.resolveCollectionRecognitionAccounts(any(Collection.class)))
                .thenReturn(new ReceivableAccountMappingPort.CollectionRecognitionAccounts("CASH-001", "CLR-001"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(200L, "SLIP-200", "DRAFT"));

        Collection firstResult = service.receivePayment(command);
        Collection secondResult = service.receivePayment(command);

        assertThat(firstResult).isNotNull();
        assertThat(secondResult).isSameAs(savedCollection);

        verify(collectionPersistencePort, times(1)).save(any(Collection.class));
        verify(journalPostingPort, times(1)).createDraftEntry(any(JournalEntryCommand.class));
    }

    @Test
    @DisplayName("수납 매칭 전표 생성 시 clearing/AR 계정 매핑 정책을 반영한다")
    void manualMatchCollectionUsesMappedAccounts() {
        Collection collection = collection();
        Receivable receivable = receivable();

        when(collectionPersistencePort.findById(20L)).thenReturn(Optional.of(collection));
        when(accountingPeriodStatusPort.isClosed(collection.getCollectionDate())).thenReturn(false);
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

        service.manualMatchCollection(new ManualMatchingCommand(20L, 30L, new BigDecimal("100.00")));

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("CLR-002", "AR-002");
        ArgumentCaptor<CollectionAllocation> allocationCaptor = ArgumentCaptor.forClass(CollectionAllocation.class);
        verify(collectionAllocationPersistencePort).save(allocationCaptor.capture());
        assertThat(allocationCaptor.getValue().getMatchedAmount()).isEqualByComparingTo("100.00");
        assertThat(allocationCaptor.getValue().getResidualCollectionAmount()).isEqualByComparingTo("0.00");
        assertThat(allocationCaptor.getValue().getResidualReceivableAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("마감된 회계기간에 대한 수납 처리 시 IllegalStateException이 발생한다")
    void receivePaymentThrowsExceptionWhenAccountingPeriodClosed() {
        CollectionCommand command = collectionCommand();
        when(accountingPeriodStatusPort.isClosed(command.collectionDate())).thenReturn(true);

        assertThatThrownBy(() -> service.receivePayment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간입니다");
    }

    @Test
    @DisplayName("차변과 대변 합계가 일치하지 않는 전표 검증 시 IllegalArgumentException이 발생한다")
    void validateJournalBalanceThrowsExceptionWhenImbalanced() {
        JournalEntryCommand imbalancedCommand = new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Imbalanced Collection Entry",
                "TEST",
                null, null, "user", "user",
                "TEST", "1",
                java.util.List.of(
                        new com.ho.account.contracts.journal.JournalLineCommand("DEBIT", "10100", new BigDecimal("500.00"), null, null, "C001", "Cash"),
                        new com.ho.account.contracts.journal.JournalLineCommand("CREDIT", "11100", new BigDecimal("450.00"), null, null, "C001", "AR Clearing")));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service, "validateJournalBalance", imbalancedCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("차변 합계(500.00)와 대변 합계(450.00)가 일치하지 않습니다");
    }

    private CollectionCommand collectionCommand() {
        return new CollectionCommand(
                LocalDate.of(2026, 5, 29),
                "C001",
                new BigDecimal("100.00"),
                null,
                null,
                null);
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