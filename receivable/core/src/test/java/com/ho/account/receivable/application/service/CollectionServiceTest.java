package com.ho.account.receivable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
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
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.domain.SalesInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
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
        assertThat(commandCaptor.getValue().operationKey()).isEqualTo("PRIMARY");
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
        SalesInvoice invoice = invoice();
        receivable.setSalesInvoice(invoice);

        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(accountingPeriodStatusPort.isClosed(collection.getCollectionDate())).thenReturn(false);
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(receivable));
        when(receivablePersistencePort.save(receivable)).thenReturn(receivable);
        when(collectionPersistencePort.save(collection)).thenReturn(collection);
        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer()));
        when(receivableAccountMappingPort.resolveCollectionMatchAccounts(collection, receivable))
                .thenReturn(new ReceivableAccountMappingPort.CollectionMatchAccounts("CLR-002", "AR-002"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        when(journalPostingPort.createDraftEntry(any()))
                .thenReturn(new JournalPostingResult(300L, "SLIP-300", "DRAFT"));

        service.manualMatchCollection(new ManualMatchingCommand(20L, 30L, new BigDecimal("100.00"), "payment-1"));

        ArgumentCaptor<JournalEntryCommand> commandCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(commandCaptor.capture());

        assertThat(commandCaptor.getValue().lines()).extracting("accountCode")
                .containsExactly("CLR-002", "AR-002");
        assertThat(commandCaptor.getValue().operationKey()).isEqualTo("MANUAL:payment-1");
        assertThat(commandCaptor.getValue().lines()).extracting(JournalLineCommand::businessPartnerCode)
                .containsExactly("C001", "C001");
        assertThat(collection.getMatchedAmount()).isEqualByComparingTo("100.00");
        assertThat(collection.getUnallocatedAmount()).isEqualByComparingTo("0.00");
        assertThat(collection.getStatus()).isEqualTo(CollectionStatus.MATCHED);
        assertThat(receivable.getOutstandingAmount()).isEqualByComparingTo("400.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PARTIAL_PAID);
        assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.PARTIAL_PAID);
        verify(salesInvoicePersistencePort).save(invoice);
        ArgumentCaptor<CollectionAllocation> allocationCaptor = ArgumentCaptor.forClass(CollectionAllocation.class);
        verify(collectionAllocationPersistencePort).save(allocationCaptor.capture());
        assertThat(allocationCaptor.getValue().getMatchedAmount()).isEqualByComparingTo("100.00");
        assertThat(allocationCaptor.getValue().getOperationId()).isEqualTo("MANUAL:payment-1");
        assertThat(allocationCaptor.getValue().getResidualCollectionAmount()).isEqualByComparingTo("0.00");
        assertThat(allocationCaptor.getValue().getResidualReceivableAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("동일 수동 매칭 작업 ID 재시도는 잔액과 전표를 다시 변경하지 않는다")
    void manualMatchRetryReusesAllocation() {
        Collection collection = collection();
        Receivable receivable = receivable();
        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(receivable));
        // The source becomes unavailable after the committed first allocation; replay must not query it.
        when(masterDataQueryPort.findBusinessPartner("C001"))
                .thenReturn(Optional.of(customer()), Optional.empty());
        when(receivableAccountMappingPort.resolveCollectionMatchAccounts(collection, receivable))
                .thenReturn(new ReceivableAccountMappingPort.CollectionMatchAccounts("CLR-002", "AR-002"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        ManualMatchingCommand request = new ManualMatchingCommand(20L, 30L, new BigDecimal("100.00"), "request-7");

        service.manualMatchCollection(request);
        ArgumentCaptor<CollectionAllocation> allocationCaptor = ArgumentCaptor.forClass(CollectionAllocation.class);
        verify(collectionAllocationPersistencePort).save(allocationCaptor.capture());
        when(collectionAllocationPersistencePort.findByCollectionIdAndOperationId(20L, "MANUAL:request-7"))
                .thenReturn(Optional.of(allocationCaptor.getValue()));

        service.manualMatchCollection(request);

        assertThat(collection.getMatchedAmount()).isEqualByComparingTo("100.00");
        assertThat(receivable.getOutstandingAmount()).isEqualByComparingTo("400.00");
        verify(journalPostingPort, times(1)).createDraftEntry(any());
        verify(collectionAllocationPersistencePort, times(1)).save(any());
        verify(masterDataQueryPort, times(1)).findBusinessPartner("C001");
    }

    @Test
    @DisplayName("원장 성공 뒤 로컬 롤백 재시도에서 고객 이름이 바뀌어도 같은 수동 전표 내용을 보낸다")
    void manualRemoteRetryHasStableDescriptionAfterCustomerRename() {
        Collection firstCollection = collection();
        Collection retriedCollection = collection();
        Receivable firstReceivable = receivable();
        Receivable retriedReceivable = receivable();
        when(collectionPersistencePort.findByIdForUpdate(20L))
                .thenReturn(Optional.of(firstCollection), Optional.of(retriedCollection));
        when(receivablePersistencePort.findByIdForUpdate(30L))
                .thenReturn(Optional.of(firstReceivable), Optional.of(retriedReceivable));
        when(masterDataQueryPort.findBusinessPartner("C001"))
                .thenReturn(Optional.of(customer()),
                        Optional.of(new BusinessPartnerRef("C001", "Renamed Customer", "CUSTOMER", true)));
        when(receivableAccountMappingPort.resolveCollectionMatchAccounts(any(), any()))
                .thenReturn(new ReceivableAccountMappingPort.CollectionMatchAccounts("CLR-002", "AR-002"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
        ManualMatchingCommand request = new ManualMatchingCommand(20L, 30L, new BigDecimal("100.00"), "request-7");

        service.manualMatchCollection(request);
        service.manualMatchCollection(request);

        ArgumentCaptor<JournalEntryCommand> journalCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort, times(2)).createDraftEntry(journalCaptor.capture());
        assertThat(journalCaptor.getAllValues()).extracting(JournalEntryCommand::operationKey)
                .containsExactly("MANUAL:request-7", "MANUAL:request-7");
        assertThat(journalCaptor.getAllValues()).extracting(JournalEntryCommand::description)
                .containsExactly("Match: C001 - 100.00", "Match: C001 - 100.00");
    }

    @Test
    @DisplayName("동일 작업 ID의 다른 채권 또는 금액은 명확한 충돌로 거부한다")
    void manualMatchRejectsChangedRetry() {
        Collection collection = collection();
        CollectionAllocation existing = CollectionAllocation.record(collection, receivable(),
                new BigDecimal("100.00"), "MANUAL:request-7");
        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(collectionAllocationPersistencePort.findByCollectionIdAndOperationId(any(), anyString()))
                .thenAnswer(invocation -> "MANUAL:request-7".equals(invocation.getArgument(1))
                        ? Optional.of(existing) : Optional.empty());

        assertThatThrownBy(() -> service.manualMatchCollection(
                new ManualMatchingCommand(20L, 30L, new BigDecimal("99.00"), "request-7")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("conflicts");
        assertThatThrownBy(() -> service.manualMatchCollection(
                new ManualMatchingCommand(20L, 31L, new BigDecimal("100.00"), "request-7")))
                .isInstanceOf(ReceivableOperationConflictException.class).hasMessageContaining("conflicts");
        verifyNoInteractions(receivablePersistencePort);
        verify(journalPostingPort, never()).createDraftEntry(any());
    }

    @Test
    @DisplayName("기존 작업 ID를 다른 고객 채권으로 재사용하면 고객 검사보다 작업 충돌을 먼저 보고한다")
    void reusedOperationWithDifferentCustomerReceivableIsConflict() {
        Collection collection = collection();
        CollectionAllocation existing = CollectionAllocation.record(collection, receivable(),
                new BigDecimal("100.00"), "MANUAL:request-7");
        Receivable otherCustomerReceivable = receivable();
        otherCustomerReceivable.setId(31L);
        otherCustomerReceivable.setCustomerCode("C002");
        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findByIdForUpdate(31L)).thenReturn(Optional.of(otherCustomerReceivable));
        when(collectionAllocationPersistencePort.findByCollectionIdAndOperationId(any(), anyString()))
                .thenAnswer(invocation -> "MANUAL:request-7".equals(invocation.getArgument(1))
                        ? Optional.of(existing) : Optional.empty());

        assertThatThrownBy(() -> service.manualMatchCollection(
                new ManualMatchingCommand(20L, 31L, new BigDecimal("100.00"), "new-cross")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer codes must match");
        assertThatThrownBy(() -> service.manualMatchCollection(
                new ManualMatchingCommand(20L, 31L, new BigDecimal("100.00"), "request-7")))
                .isInstanceOf(ReceivableOperationConflictException.class)
                .hasMessageContaining("Matching operation ID conflicts");
        verify(receivablePersistencePort, times(1)).findByIdForUpdate(31L);
        verifyNoInteractions(journalPostingPort, masterDataQueryPort);
        verify(collectionAllocationPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("수동 매칭은 재시도에 사용할 작업 ID를 요구한다")
    void manualMatchRequiresOperationId() {
        assertThatThrownBy(() -> new ManualMatchingCommand(20L, 30L, new BigDecimal("100.00"), " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("operationId");
    }

    @Test
    @DisplayName("분수 센트 금액은 저장이나 전표 생성 전에 거부하고 정확한 두 자리 값은 허용한다")
    void manualMatchRejectsUnrepresentableAmount() {
        assertThatThrownBy(() -> new ManualMatchingCommand(20L, 30L, new BigDecimal("1.001"), "fraction"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("two decimal places");
        assertThat(new ManualMatchingCommand(20L, 30L, new BigDecimal("1.000"), "exact")
                .matchingAmount()).isEqualTo(new BigDecimal("1.00"));
        verifyNoInteractions(collectionPersistencePort, receivablePersistencePort, journalPostingPort);
    }

    @Test
    @DisplayName("같은 수납과 채권의 별도 부분 매칭은 작업 ID별로 두 전표를 만든다")
    void separateManualAllocationsHaveDistinctJournalOperations() {
        Collection collection = collection();
        collection.setAmount(new BigDecimal("300.00"));
        Receivable receivable = receivable();
        stubManualMatch(collection, receivable);

        service.manualMatchCollection(new ManualMatchingCommand(20L, 30L, new BigDecimal("100.00"), "first"));
        service.manualMatchCollection(new ManualMatchingCommand(20L, 30L, new BigDecimal("100.00"), "second"));

        ArgumentCaptor<JournalEntryCommand> journalCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort, times(2)).createDraftEntry(journalCaptor.capture());
        assertThat(journalCaptor.getAllValues()).extracting(JournalEntryCommand::operationKey)
                .containsExactly("MANUAL:first", "MANUAL:second");
        assertThat(journalCaptor.getAllValues()).extracting(JournalEntryCommand::lineageSourceId)
                .containsExactly("20", "20");
    }

    @Test
    @DisplayName("자동 매칭의 롤백 후 재시도는 같은 전표 작업 ID를 사용한다")
    void autoMatchRetryUsesStableOperationKey() {
        Collection first = collection();
        Collection retry = collection();
        Receivable firstReceivable = receivable();
        Receivable retryReceivable = receivable();
        when(collectionPersistencePort.findByIdForUpdate(20L))
                .thenReturn(Optional.of(first), Optional.of(retry));
        when(receivablePersistencePort.findOpenItemsByCustomerCode("C001"))
                .thenReturn(List.of(firstReceivable), List.of(retryReceivable));
        when(collectionMatchingPolicyPort.selectMatch(any(), any()))
                .thenReturn(Optional.of(firstReceivable), Optional.of(retryReceivable));
        when(receivablePersistencePort.findByIdForUpdate(30L))
                .thenReturn(Optional.of(firstReceivable), Optional.of(retryReceivable));
        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer()));
        when(receivableAccountMappingPort.resolveCollectionMatchAccounts(any(), any()))
                .thenReturn(new ReceivableAccountMappingPort.CollectionMatchAccounts("CLR-002", "AR-002"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));

        service.attemptAutoMatching(20L);
        service.attemptAutoMatching(20L);

        ArgumentCaptor<JournalEntryCommand> journalCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort, times(2)).createDraftEntry(journalCaptor.capture());
        assertThat(journalCaptor.getAllValues()).extracting(JournalEntryCommand::operationKey)
                .containsExactly("AUTO:30", "AUTO:30");
    }

    @Test
    @DisplayName("다른 수납이 먼저 차감한 채권은 잠금 후 최신 잔액으로 재검증하여 초과 배분을 거부한다")
    void autoMatchRejectsStaleCandidateAfterReceivableLock() {
        Collection collection = collection();
        Receivable staleCandidate = receivable();
        Receivable lockedReceivable = receivable();
        lockedReceivable.setOutstandingAmount(new BigDecimal("50.00"));
        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findOpenItemsByCustomerCode("C001"))
                .thenReturn(List.of(staleCandidate));
        when(collectionMatchingPolicyPort.selectMatch(collection, List.of(staleCandidate)))
                .thenReturn(Optional.of(staleCandidate));
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(lockedReceivable));

        assertThatThrownBy(() -> service.attemptAutoMatching(20L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("채권 잔액");

        assertThat(collection.getMatchedAmount()).isEqualByComparingTo("0.00");
        assertThat(lockedReceivable.getOutstandingAmount()).isEqualByComparingTo("50.00");
        verify(collectionPersistencePort, never()).save(any());
        verify(receivablePersistencePort, never()).save(any());
        verifyNoInteractions(collectionAllocationPersistencePort, journalPostingPort);
    }

    private void stubManualMatch(Collection collection, Receivable receivable) {
        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(receivable));
        stubMatchPosting(collection, receivable);
    }

    @Test
    @DisplayName("다른 고객의 채권은 수동 매칭 전에 거부하고 잔액, 인보이스, 저장, 전표를 건드리지 않는다")
    void manualMatchRejectsDifferentCustomerBeforeMutation() {
        Collection collection = collection();
        Receivable receivable = receivable();
        receivable.setCustomerCode("C002");
        SalesInvoice invoice = invoice();
        invoice.setCustomerCode("C002");
        receivable.setSalesInvoice(invoice);

        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(receivable));

        assertThatThrownBy(() -> service.manualMatchCollection(
                new ManualMatchingCommand(20L, 30L, new BigDecimal("40.00"), "cross-customer")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(collection.getMatchedAmount()).isEqualByComparingTo("0.00");
        assertThat(collection.getUnallocatedAmount()).isEqualByComparingTo("100.00");
        assertThat(collection.getStatus()).isEqualTo(CollectionStatus.RECEIVED);
        assertThat(receivable.getOutstandingAmount()).isEqualByComparingTo("500.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
        assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.ISSUED);
        // 조회 성공 뒤에도 어떤 쓰기 포트나 전표 준비 포트에도 도달하지 않아야 한다.
        verify(collectionPersistencePort, never()).save(any());
        verify(receivablePersistencePort, never()).save(any());
        verify(collectionAllocationPersistencePort, never()).save(any());
        verifyNoInteractions(salesInvoicePersistencePort,
                receivableAccountMappingPort, journalPostingPort, masterDataQueryPort);
    }

    @Test
    @DisplayName("자동 매칭 정책이 다른 고객의 채권을 반환해도 공통 검증이 상태 변경 전에 거부한다")
    void autoMatchRejectsDifferentCustomerCandidateBeforeMutation() {
        Collection collection = collection();
        Receivable receivable = receivable();
        receivable.setCustomerCode("C002");
        SalesInvoice invoice = invoice();
        invoice.setCustomerCode("C002");
        receivable.setSalesInvoice(invoice);

        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findOpenItemsByCustomerCode("C001"))
                .thenReturn(java.util.List.of(receivable));
        when(collectionMatchingPolicyPort.selectMatch(collection, java.util.List.of(receivable)))
                .thenReturn(Optional.of(receivable));
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(receivable));

        assertThatThrownBy(() -> service.attemptAutoMatching(20L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(collection.getMatchedAmount()).isEqualByComparingTo("0.00");
        assertThat(collection.getStatus()).isEqualTo(CollectionStatus.RECEIVED);
        assertThat(receivable.getOutstandingAmount()).isEqualByComparingTo("500.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
        assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.ISSUED);
        verify(collectionPersistencePort, never()).save(any());
        verify(receivablePersistencePort, never()).save(any());
        verifyNoInteractions(salesInvoicePersistencePort, collectionAllocationPersistencePort,
                receivableAccountMappingPort, journalPostingPort, masterDataQueryPort);
    }

    @Test
    @DisplayName("같은 고객의 수동 부분 매칭은 양쪽 잔액과 인보이스를 부분 상태로 바꾼다")
    void manualPartialMatchPreservesBalancesAndInvoiceStatus() {
        Collection collection = collection();
        Receivable receivable = receivable();
        SalesInvoice invoice = invoice();
        receivable.setSalesInvoice(invoice);
        stubMatchPosting(collection, receivable);
        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(receivable));

        service.manualMatchCollection(new ManualMatchingCommand(20L, 30L, new BigDecimal("40.00"), "partial-match"));

        assertThat(collection.getMatchedAmount()).isEqualByComparingTo("40.00");
        assertThat(collection.getUnallocatedAmount()).isEqualByComparingTo("60.00");
        assertThat(collection.getStatus()).isEqualTo(CollectionStatus.PARTIAL_MATCHED);
        assertThat(receivable.getOutstandingAmount()).isEqualByComparingTo("460.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PARTIAL_PAID);
        assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.PARTIAL_PAID);
        verify(salesInvoicePersistencePort).save(invoice);
        assertPartialMatchAllocationAndJournal("40.00", "60.00", "460.00");
    }

    @Test
    @DisplayName("같은 고객의 자동 부분 매칭은 남은 수납액만 배분하고 채권과 인보이스를 부분 상태로 바꾼다")
    void autoPartialMatchPreservesBalancesAndInvoiceStatus() {
        Collection collection = collection();
        collection.setMatchedAmount(new BigDecimal("40.00"));
        collection.setStatus(CollectionStatus.PARTIAL_MATCHED);
        Receivable receivable = receivable();
        SalesInvoice invoice = invoice();
        receivable.setSalesInvoice(invoice);
        stubMatchPosting(collection, receivable);
        when(collectionPersistencePort.findByIdForUpdate(20L)).thenReturn(Optional.of(collection));
        when(receivablePersistencePort.findOpenItemsByCustomerCode("C001"))
                .thenReturn(java.util.List.of(receivable));
        when(collectionMatchingPolicyPort.selectMatch(collection, java.util.List.of(receivable)))
                .thenReturn(Optional.of(receivable));
        when(receivablePersistencePort.findByIdForUpdate(30L)).thenReturn(Optional.of(receivable));

        service.attemptAutoMatching(20L);

        assertThat(collection.getMatchedAmount()).isEqualByComparingTo("100.00");
        assertThat(collection.getUnallocatedAmount()).isEqualByComparingTo("0.00");
        assertThat(collection.getStatus()).isEqualTo(CollectionStatus.MATCHED);
        assertThat(receivable.getOutstandingAmount()).isEqualByComparingTo("440.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PARTIAL_PAID);
        assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.PARTIAL_PAID);
        verify(salesInvoicePersistencePort).save(invoice);
        assertPartialMatchAllocationAndJournal("60.00", "0.00", "440.00");
    }

    private void stubMatchPosting(Collection collection, Receivable receivable) {
        when(masterDataQueryPort.findBusinessPartner("C001")).thenReturn(Optional.of(customer()));
        when(receivableAccountMappingPort.resolveCollectionMatchAccounts(collection, receivable))
                .thenReturn(new ReceivableAccountMappingPort.CollectionMatchAccounts("CLR-002", "AR-002"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("account", "Account", false, false)));
    }

    private void assertPartialMatchAllocationAndJournal(String amount, String collectionBalance,
                                                         String receivableBalance) {
        ArgumentCaptor<CollectionAllocation> allocationCaptor = ArgumentCaptor.forClass(CollectionAllocation.class);
        verify(collectionAllocationPersistencePort).save(allocationCaptor.capture());
        assertThat(allocationCaptor.getValue().getMatchedAmount()).isEqualByComparingTo(amount);
        assertThat(allocationCaptor.getValue().getResidualCollectionAmount()).isEqualByComparingTo(collectionBalance);
        assertThat(allocationCaptor.getValue().getResidualReceivableAmount()).isEqualByComparingTo(receivableBalance);

        ArgumentCaptor<JournalEntryCommand> journalCaptor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(journalPostingPort).createDraftEntry(journalCaptor.capture());
        assertThat(journalCaptor.getValue().lines()).extracting(JournalLineCommand::amount)
                .containsExactly(new BigDecimal(amount), new BigDecimal(amount));
        assertThat(journalCaptor.getValue().lines()).extracting(JournalLineCommand::businessPartnerCode)
                .containsExactly("C001", "C001");
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
    private SalesInvoice invoice() {
        SalesInvoice invoice = new SalesInvoice();
        invoice.setCustomerCode("C001");
        invoice.setStatus(SalesInvoiceStatus.ISSUED);
        return invoice;
    }
}
