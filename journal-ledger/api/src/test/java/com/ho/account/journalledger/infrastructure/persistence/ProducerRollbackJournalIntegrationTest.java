package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.contracts.journal.*;
import com.ho.account.contracts.masterdata.*;
import com.ho.account.expenditure.application.port.in.AdvancePaymentCommand;
import com.ho.account.expenditure.application.port.in.OffsetPayableCommand;
import com.ho.account.expenditure.application.service.PaymentService;
import com.ho.account.expenditure.application.service.PayableOperationConflictException;
import com.ho.account.expenditure.domain.*;
import com.ho.account.expenditure.payable.infrastructure.adapter.HttpPayableJournalAdapter;
import com.ho.account.journalledger.adapter.in.web.journal.JournalPostingRestController;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalCreationOperationPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.*;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.infrastructure.adapter.JournalPostingAdapter;
import com.ho.account.receivable.application.port.in.*;
import com.ho.account.receivable.application.port.out.*;
import com.ho.account.receivable.application.service.*;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionAllocation;
import com.ho.account.receivable.domain.Receivable;
import com.ho.account.receivable.domain.ReceivableStatus;
import com.ho.account.receivable.domain.SalesInvoice;
import com.ho.account.receivable.infrastructure.adapter.HttpReceivableJournalAdapter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/**
 * Actual producer services run in a local JDBC transaction over synthetic source port fixtures.
 * The HTTP transport invokes actual JSON binding, trusted-maker REST controller and Journal service/JPA
 * registry on a separate thread/connection. Synthetic trusted headers stand in for the Gateway boundary.
 * An IOException is injected only AFTER Journal has committed. No server or external DB is contacted.
 */
@SpringBootTest(classes = ProducerRollbackJournalIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=producer-rollback-test",
        "logging.config=classpath:producer-rollback-logback.xml",
        "spring.datasource.url=jdbc:h2:mem:producer-rollback-journal;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true", "spring.flyway.locations=classpath:db/journal-migration",
        "spring.sql.init.mode=never", "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false", "spring.cloud.vault.enabled=false", "eureka.client.enabled=false"
})
class ProducerRollbackJournalIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 25);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    @Autowired private JournalEntryService journals;
    @Autowired private JdbcTemplate journalJdbc;
    private JdbcTemplate source;
    private TransactionTemplate local;
    private MockMvc http;
    private ExecutorService remote;
    private final AtomicBoolean loseResponse = new AtomicBoolean();
    private final AtomicBoolean holdResponse = new AtomicBoolean();
    private CountDownLatch remoteCommitted;
    private CountDownLatch releaseResponse;
    private final AtomicInteger remoteCalls = new AtomicInteger();
    private final List<Long> allocatedIds = new CopyOnWriteArrayList<>();
    private CollectionService collections;
    private SalesService sales;
    private PaymentService payments;
    private com.ho.account.contracts.source.SourceDocumentProvider arSources;
    private com.ho.account.contracts.source.SourceDocumentProvider apSources;
    private SourceCreationPersistencePort receivableReceipts;
    private com.ho.account.expenditure.application.port.out.SourceCreationPersistencePort payableReceipts;

    @BeforeEach
    void setup() throws Exception {
        journalJdbc.update("DELETE FROM journal_creation_operations");
        journalJdbc.update("DELETE FROM journal_details");
        journalJdbc.update("DELETE FROM journal_entries");
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:producer-source-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        source = new JdbcTemplate(ds);
        local = new TransactionTemplate(new DataSourceTransactionManager(ds));
        source.execute("CREATE TABLE source_documents (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,"
                + " kind VARCHAR(100) NOT NULL, payload TEXT NOT NULL)");
        source.execute("CREATE TABLE source_allocations (source_id BIGINT, operation_id VARCHAR(100), payload TEXT,"
                + " PRIMARY KEY(source_id, operation_id))");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/receivable-migration/V3__source_creation_receipts.sql"),
                new ClassPathResource("db/payable-migration/V3__source_creation_receipts.sql")).execute(ds);
        receivableReceipts = new com.ho.account.receivable.adapter.out.persistence.SourceCreationPersistenceAdapter(source);
        payableReceipts = new com.ho.account.expenditure.adapter.out.persistence.SourceCreationPersistenceAdapter(source);
        remote = Executors.newSingleThreadExecutor();
        http = MockMvcBuilders.standaloneSetup(new JournalPostingRestController(new JournalPostingAdapter(journals)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
        restartProducers();
    }

    @AfterEach
    void stopRemote() {
        remote.shutdownNow();
    }

    private void restartProducers() throws Exception {
        MasterDataQueryPort master = mock(MasterDataQueryPort.class);
        when(master.findBusinessPartner(anyString())).thenAnswer(call ->
                Optional.of(new BusinessPartnerRef(call.getArgument(0), "Synthetic", "BOTH", true)));
        when(master.findAccountSubject(anyString())).thenReturn(
                Optional.of(new AccountSubjectRef("10100", "Synthetic", false, false)));
        ReceivableAccountMappingPort arAccounts = mock(ReceivableAccountMappingPort.class);
        when(arAccounts.resolveCollectionRecognitionAccounts(any())).thenReturn(
                new ReceivableAccountMappingPort.CollectionRecognitionAccounts("10100", "20100"));
        when(arAccounts.resolveSalesRecognitionAccounts(any())).thenReturn(
                new ReceivableAccountMappingPort.SalesRecognitionAccounts("10100", "40100", "20200"));
        when(arAccounts.resolveCollectionMatchAccounts(any(), any())).thenReturn(
                new ReceivableAccountMappingPort.CollectionMatchAccounts("20100", "10100"));
        var apAccounts = mock(com.ho.account.expenditure.application.port.out.PayableAccountMappingPort.class);
        when(apAccounts.resolveAdvancePaymentAccounts(any())).thenReturn(
                new com.ho.account.expenditure.application.port.out.PayableAccountMappingPort.AdvancePaymentAccounts("10100", "20100"));
        when(apAccounts.resolveAdvanceOffsetAccounts(any())).thenReturn(
                new com.ho.account.expenditure.application.port.out.PayableAccountMappingPort.AdvanceOffsetAccounts("20100", "10100"));

        CollectionPersistencePort cp = mock(CollectionPersistencePort.class);
        when(cp.save(any())).thenAnswer(call -> save(call.getArgument(0)));
        when(cp.findById(anyLong())).thenAnswer(call -> find(Collection.class, call.getArgument(0), false));
        when(cp.findByIdForUpdate(anyLong())).thenAnswer(call -> find(Collection.class, call.getArgument(0), true));
        when(cp.findByReferenceNo(anyString())).thenAnswer(call -> all(Collection.class).stream()
                .filter(c -> c.getReferenceNo().equals(call.getArgument(0))).findFirst());
        SalesInvoicePersistencePort sp = mock(SalesInvoicePersistencePort.class);
        when(sp.save(any())).thenAnswer(call -> save(call.getArgument(0)));
        when(sp.findById(anyLong())).thenAnswer(call -> find(SalesInvoice.class, call.getArgument(0), false));
        when(sp.findByInvoiceNo(anyString())).thenAnswer(call -> all(SalesInvoice.class).stream()
                .filter(i -> i.getInvoiceNo().equals(call.getArgument(0))).findFirst());
        ReceivablePersistencePort rp = mock(ReceivablePersistencePort.class);
        when(rp.save(any())).thenAnswer(call -> save(call.getArgument(0)));
        when(rp.findByIdForUpdate(anyLong())).thenAnswer(call -> find(Receivable.class, call.getArgument(0), true));
        when(rp.findOpenItemsByCustomerCode(anyString())).thenAnswer(call -> all(Receivable.class).stream()
                .filter(r -> r.getCustomerCode().equals(call.getArgument(0)) && r.getOutstandingAmount().signum() > 0).toList());
        CollectionAllocationPersistencePort allocations = mock(CollectionAllocationPersistencePort.class);
        when(allocations.save(any())).thenAnswer(call -> {
            CollectionAllocation a = call.getArgument(0);
            source.update("INSERT INTO source_allocations VALUES (?, ?, ?)",
                    a.getCollection().getId(), a.getOperationId(), json.writeValueAsString(a));
            return a;
        });
        when(allocations.findByCollectionIdAndOperationId(anyLong(), anyString())).thenAnswer(call ->
                allocation(call.getArgument(0), call.getArgument(1), CollectionAllocation.class));
        JournalPostingPort arRemote = client(HttpReceivableJournalAdapter.class);
        collections = new CollectionService(cp, rp, sp, master, arRemote, arAccounts,
                new com.ho.account.receivable.adapter.out.policy.ReferenceFirstCollectionMatchingPolicy(), allocations, date -> false, receivableReceipts);
        sales = new SalesService(sp, rp, master, arRemote, arAccounts, date -> false, receivableReceipts);
        arSources = new com.ho.account.receivable.adapter.out.source.ReceivableCreationSourceDocumentProvider(
                receivableReceipts, sp, cp);

        var advances = mock(com.ho.account.expenditure.application.port.out.AdvancePaymentPersistencePort.class);
        when(advances.save(any())).thenAnswer(call -> save(call.getArgument(0)));
        when(advances.findById(anyLong())).thenAnswer(call -> find(AdvancePayment.class, call.getArgument(0), false));
        when(advances.findByIdForUpdate(anyLong())).thenAnswer(call -> find(AdvancePayment.class, call.getArgument(0), true));
        apSources = new com.ho.account.expenditure.adapter.out.source.AdvanceCreationSourceDocumentProvider(
                payableReceipts, advances);
        var payables = mock(com.ho.account.expenditure.application.port.out.PayablePersistencePort.class);
        when(payables.save(any())).thenAnswer(call -> save(call.getArgument(0)));
        when(payables.findByIdForUpdate(anyLong())).thenAnswer(call -> find(Payable.class, call.getArgument(0), true));
        var offsets = mock(com.ho.account.expenditure.application.port.out.PayableOffsetOperationPersistencePort.class);
        when(offsets.findByPayableIdAndOperationId(anyLong(), anyString())).thenAnswer(call ->
                allocation(call.getArgument(0), call.getArgument(1), PayableOffsetOperation.class));
        doAnswer(call -> {
            PayableOffsetOperation op = call.getArgument(0);
            source.update("INSERT INTO source_allocations VALUES (?, ?, ?)",
                    op.payableId(), op.operationId(), json.writeValueAsString(op));
            return null;
        }).when(offsets).save(any());
        payments = new PaymentService(
                mock(com.ho.account.expenditure.application.port.out.PaymentPersistencePort.class), payables,
                mock(com.ho.account.expenditure.application.port.out.PaymentRunPersistencePort.class), advances,
                offsets, master, client(HttpPayableJournalAdapter.class), apAccounts,
                mock(com.ho.account.expenditure.application.port.out.PaymentExecutionPort.class), date -> false, payableReceipts);
    }

    private <T extends JournalPostingPort> T client(Class<T> type) throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://synthetic-journal.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(ExpectedCount.manyTimes(), requestTo("http://synthetic-journal.test/api/v1/journals/posting"))
                .andRespond(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    remoteCalls.incrementAndGet();
                    try {
                        MockHttpServletResponse result = remote.submit(() -> postJournal(body)).get(10, TimeUnit.SECONDS);
                        if (result.getStatus() == 200) {
                            if (holdResponse.compareAndSet(true, false)) {
                                remoteCommitted.countDown();
                                if (!releaseResponse.await(10, TimeUnit.SECONDS)) throw new IOException("Response gate timed out");
                            }
                            if (loseResponse.compareAndSet(true, false)) throw new IOException("Response lost after remote COMMIT");
                        }
                        return withStatus(HttpStatus.valueOf(result.getStatus())).body(result.getContentAsString())
                                .contentType(MediaType.APPLICATION_JSON).createResponse(request);
                    } catch (ExecutionException exception) {
                        throw new IOException(exception.getCause());
                    } catch (InterruptedException | TimeoutException exception) {
                        throw new IOException(exception);
                    }
                });
        // Use the adapter's existing package-private transport seam; production constructors are unchanged.
        var constructor = type.getDeclaredConstructor(RestClient.class);
        constructor.setAccessible(true);
        return constructor.newInstance(builder.build());
    }

    @Test
    void collectionRollbackReallocatedIdAndRestartReuseRemoteReceiptAndRejectChangedContent() throws Exception {
        CollectionCommand request = collection("bank-deposit-765", "100.00");
        loseResponse.set(true);
        assertThatThrownBy(() -> tx(() -> collections.receivePayment(request))).isInstanceOf(IllegalStateException.class);
        assertEmptySourceAndOneJournal();
        assertThat(tx(() -> arSources.getSourceDocument("COLLECTION_REF", request.referenceNo()))).isEmpty();
        long rolledBackId = allocatedIds.get(0);
        assertThatThrownBy(() -> tx(() -> collections.receivePayment(collection("bank-deposit-765", "101.00"))))
                .isInstanceOf(ReceivableOperationConflictException.class);
        assertEmptySourceAndOneJournal();
        restartProducers();
        Collection saved = tx(() -> collections.receivePayment(collection("bank-deposit-765", "100.000")));
        assertThat(saved.getId()).isNotEqualTo(rolledBackId);
        assertThat(tx(() -> arSources.getSourceDocument("COLLECTION_REF", request.referenceNo())).orElseThrow())
                .containsEntry("id", saved.getId());
        int calls = remoteCalls.get();
        assertThat(tx(() -> collections.receivePayment(request)).getId()).isEqualTo(saved.getId());
        assertThat(remoteCalls.get()).isEqualTo(calls);
        assertThatThrownBy(() -> tx(() -> collections.receivePayment(new CollectionCommand(DATE, "C1",
                new BigDecimal("100"), "different-bank", null, request.referenceNo()))))
                .isInstanceOf(ReceivableOperationConflictException.class);
        assertThat(count("source_documents")).isOne();
        assertThat(count("receivable_source_creations")).isOne();
        assertOneJournal();
    }

    @Test
    void salesRollbackReallocatedIdBindsDueDateAndAmountAndPreservesOneReceivable() throws Exception {
        SalesInvoiceCommand request = invoice("INV-765", "100.00", DATE.plusDays(30));
        loseResponse.set(true);
        assertThatThrownBy(() -> tx(() -> sales.createSalesInvoice(request))).isInstanceOf(IllegalStateException.class);
        assertEmptySourceAndOneJournal();
        long failedId = allocatedIds.get(0);
        for (SalesInvoiceCommand changed : List.of(invoice("INV-765", "101.00", DATE.plusDays(30)),
                invoice("INV-765", "100.00", DATE.plusDays(31)))) {
            assertThatThrownBy(() -> tx(() -> sales.createSalesInvoice(changed)))
                    .isInstanceOf(ReceivableOperationConflictException.class);
            assertEmptySourceAndOneJournal();
        }
        restartProducers();
        SalesInvoice saved = tx(() -> sales.createSalesInvoice(request));
        assertThat(tx(() -> arSources.getSourceDocument("SALES_INVOICE_REF", request.invoiceNo())).orElseThrow())
                .containsEntry("id", saved.getId());
        assertThat(saved.getId()).isNotEqualTo(failedId);
        assertThat(tx(() -> sales.createSalesInvoice(request)).getId()).isEqualTo(saved.getId());
        assertThatThrownBy(() -> tx(() -> sales.createSalesInvoice(invoice("INV-765", "101.00", DATE.plusDays(30)))))
                .isInstanceOf(ReceivableOperationConflictException.class);
        assertThat(all(SalesInvoice.class)).hasSize(1);
        assertThat(all(Receivable.class)).hasSize(1);
        assertOneJournal();
    }

    @Test
    void advanceRollbackReallocatedIdBindsDescriptionAndAmountAcrossRestart() throws Exception {
        AdvancePaymentCommand request = advance("advance-contract-765", "200.00", "contract deposit");
        loseResponse.set(true);
        assertThatThrownBy(() -> tx(() -> payments.recordAdvancePayment(request))).isInstanceOf(IllegalStateException.class);
        assertEmptySourceAndOneJournal();
        long failedId = allocatedIds.get(0);
        for (AdvancePaymentCommand changed : List.of(advance("advance-contract-765", "201.00", "contract deposit"),
                advance("advance-contract-765", "200.00", "changed purpose"))) {
            assertThatThrownBy(() -> tx(() -> payments.recordAdvancePayment(changed)))
                    .isInstanceOf(PayableOperationConflictException.class);
            assertEmptySourceAndOneJournal();
        }
        restartProducers();
        AdvancePayment saved = tx(() -> payments.recordAdvancePayment(request));
        assertThat(tx(() -> apSources.getSourceDocument("ADVANCE_PAYMENT_REF", request.operationId())).orElseThrow())
                .containsEntry("id", saved.getId());
        assertThat(saved.getId()).isNotEqualTo(failedId);
        assertThat(tx(() -> payments.recordAdvancePayment(request)).getId()).isEqualTo(saved.getId());
        assertThatThrownBy(() -> tx(() -> payments.recordAdvancePayment(advance("advance-contract-765", "201.00", "contract deposit"))))
                .isInstanceOf(PayableOperationConflictException.class);
        assertThat(count("payable_source_creations")).isOne();
        assertThat(count("source_documents")).isOne();
        assertOneJournal();
    }

    @Test
    void changedAdvanceAfterRemoteSuccessConflictsWithoutChangingEitherSourceBalance() {
        Payable payable = new Payable();
        payable.setVendorCode("V1"); payable.setOriginalAmount(new BigDecimal("500"));
        payable.setOutstandingAmount(new BigDecimal("500")); payable.setStatus(PayableStatus.OPEN);
        save(payable);
        AdvancePayment first = sourceAdvance(); AdvancePayment second = sourceAdvance();
        OffsetPayableCommand request = new OffsetPayableCommand(payable.getId(), first.getId(),
                new BigDecimal("100.00"), "offset-765", DATE);
        loseResponse.set(true);
        assertThatThrownBy(() -> tx(() -> payments.offsetPayableWithAdvancePayment(request)))
                .isInstanceOf(IllegalStateException.class);
        for (OffsetPayableCommand changed : List.of(
                new OffsetPayableCommand(payable.getId(), second.getId(), new BigDecimal("100.00"), "offset-765", DATE),
                new OffsetPayableCommand(payable.getId(), first.getId(), new BigDecimal("101.00"), "offset-765", DATE))) {
            assertThatThrownBy(() -> tx(() -> payments.offsetPayableWithAdvancePayment(changed)))
                    .isInstanceOf(PayableOperationConflictException.class);
            assertThat(find(Payable.class, payable.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("500");
            assertThat(find(AdvancePayment.class, first.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("200");
            assertThat(find(AdvancePayment.class, second.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("200");
            assertThat(count("source_allocations")).isZero();
            assertOneJournal();
        }
        tx(() -> payments.offsetPayableWithAdvancePayment(request));
        tx(() -> payments.offsetPayableWithAdvancePayment(request));
        assertThat(find(Payable.class, payable.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("400");
        assertThat(count("source_allocations")).isOne();
        assertOneJournal();
        tx(() -> payments.offsetPayableWithAdvancePayment(new OffsetPayableCommand(payable.getId(), second.getId(),
                new BigDecimal("50.00"), "offset-766", DATE)));
        assertThat(journalCount()).isEqualTo(2);
    }

    @Test
    void changedReceivableAfterRemoteSuccessConflictsWithoutChangingEitherSourceBalance() {
        Collection collection = new Collection();
        collection.setCollectionDate(DATE); collection.setCustomerCode("C1");
        collection.setAmount(new BigDecimal("100")); collection.setReferenceNo("match-only"); save(collection);
        Receivable first = sourceReceivable(); Receivable second = sourceReceivable();
        ManualMatchingCommand request = new ManualMatchingCommand(collection.getId(), first.getId(), new BigDecimal("40"), "match-765");
        loseResponse.set(true);
        assertThatThrownBy(() -> tx(() -> { collections.manualMatchCollection(request); return null; }))
                .isInstanceOf(IllegalStateException.class);
        for (ManualMatchingCommand changed : List.of(
                new ManualMatchingCommand(collection.getId(), second.getId(), new BigDecimal("40"), "match-765"),
                new ManualMatchingCommand(collection.getId(), first.getId(), new BigDecimal("41"), "match-765"))) {
            assertThatThrownBy(() -> tx(() -> { collections.manualMatchCollection(changed); return null; }))
                    .isInstanceOf(ReceivableOperationConflictException.class);
            assertThat(find(Collection.class, collection.getId(), false).orElseThrow().getUnallocatedAmount()).isEqualByComparingTo("100");
            assertThat(find(Receivable.class, first.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("500");
            assertThat(find(Receivable.class, second.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("500");
            assertThat(count("source_allocations")).isZero();
            assertOneJournal();
        }
        tx(() -> { collections.manualMatchCollection(request); return null; });
        tx(() -> { collections.manualMatchCollection(request); return null; });
        assertThat(count("source_allocations")).isOne();
        assertOneJournal();
        tx(() -> { collections.manualMatchCollection(new ManualMatchingCommand(collection.getId(), second.getId(),
                new BigDecimal("20.00"), "match-766")); return null; });
        assertThat(journalCount()).isEqualTo(2);
    }

    @Test
    void automaticCandidateDriftAfterRemoteSuccessConflictsWithoutSecondClearing() {
        Collection original = sourceCollection("auto-original", "100");
        Receivable first = sourceReceivable(); first.setDueDate(DATE); save(first);
        Receivable second = sourceReceivable(); second.setOutstandingAmount(new BigDecimal("100")); save(second);
        loseResponse.set(true);
        assertThatThrownBy(() -> tx(() -> { collections.attemptAutoMatching(original.getId()); return null; }))
                .isInstanceOf(IllegalStateException.class);
        assertOneJournal();
        assertThat(count("source_allocations")).isZero();

        // A different committed collection consumes A. The same automatic attempt now selects B.
        Collection other = sourceCollection("auto-other", "500");
        tx(() -> { collections.manualMatchCollection(new ManualMatchingCommand(other.getId(), first.getId(),
                new BigDecimal("500"), "other-collection")); return null; });
        assertThatThrownBy(() -> tx(() -> { collections.attemptAutoMatching(original.getId()); return null; }))
                .isInstanceOf(ReceivableOperationConflictException.class);
        assertThat(find(Collection.class, original.getId(), false).orElseThrow().getUnallocatedAmount()).isEqualByComparingTo("100");
        assertThat(find(Receivable.class, first.getId(), false).orElseThrow().getOutstandingAmount()).isZero();
        assertThat(find(Receivable.class, second.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("100");
        assertThat(count("source_allocations")).isOne();
        assertThat(journalCount()).isEqualTo(2); // Original lost response + the other collection, never B.
    }

    @Test
    void automaticUnchangedCandidateRetryCompletesTheSingleRemainderOperation() {
        Collection original = sourceCollection("auto-retry", "100");
        Receivable target = sourceReceivable(); target.setDueDate(DATE); save(target);
        loseResponse.set(true);
        assertThatThrownBy(() -> tx(() -> { collections.attemptAutoMatching(original.getId()); return null; }))
                .isInstanceOf(IllegalStateException.class);
        tx(() -> { collections.attemptAutoMatching(original.getId()); return null; });
        tx(() -> { collections.attemptAutoMatching(original.getId()); return null; });
        assertThat(find(Collection.class, original.getId(), false).orElseThrow().getUnallocatedAmount()).isZero();
        assertThat(find(Receivable.class, target.getId(), false).orElseThrow().getOutstandingAmount()).isEqualByComparingTo("400");
        assertThat(count("source_allocations")).isOne();
        assertOneJournal();
    }

    @Test
    void legacyJsonWithoutSourceDigestStillUsesItsExistingFingerprint() throws Exception {
        JournalEntryCommand command = new JournalEntryCommand(DATE, DATE, "legacy", "NORMAL", "KRW",
                BigDecimal.ONE, "untrusted", "untrusted", "LEGACY_TEST", "legacy-765", null, "MAIN_V1", List.of(
                new JournalLineCommand("DEBIT", "10100", BigDecimal.ONE, null, null, null, "debit"),
                new JournalLineCommand("CREDIT", "20100", BigDecimal.ONE, null, null, null, "credit")));
        var legacyJson = json.valueToTree(command);
        ((com.fasterxml.jackson.databind.node.ObjectNode) legacyJson).remove("sourceRequestFingerprint");
        var first = postJournal(json.writeValueAsString(legacyJson));
        var retry = postJournal(json.writeValueAsString(legacyJson));
        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(retry.getContentAsString()).isEqualTo(first.getContentAsString());
        assertOneJournal();
        ((com.fasterxml.jackson.databind.node.ObjectNode) legacyJson).put("sourceRequestFingerprint", "a".repeat(64));
        assertThat(postJournal(json.writeValueAsString(legacyJson)).getStatus()).isEqualTo(409);
        assertOneJournal();
        ((com.fasterxml.jackson.databind.node.ObjectNode) legacyJson).put("lineageSourceId", "digest-765");
        assertThat(postJournal(json.writeValueAsString(legacyJson)).getStatus()).isEqualTo(200);
        ((com.fasterxml.jackson.databind.node.ObjectNode) legacyJson).remove("sourceRequestFingerprint");
        assertThat(postJournal(json.writeValueAsString(legacyJson)).getStatus()).isEqualTo(409);
        assertThat(journalCount()).isEqualTo(2);
    }

    private Collection sourceCollection(String key, String amount) {
        Collection c = new Collection(); c.setCollectionDate(DATE); c.setCustomerCode("C1");
        c.setAmount(new BigDecimal(amount)); c.setReferenceNo(key); return save(c);
    }

    @Test
    void concurrentIdenticalCreationsSerializeSourceAndRemoteForEveryProducer() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            for (Supplier<Object> operation : List.<Supplier<Object>>of(
                    () -> collections.receivePayment(collection("concurrent-765", "100.00")),
                    () -> sales.createSalesInvoice(invoice("CONCURRENT-765", "100.00", DATE.plusDays(30))),
                    () -> payments.recordAdvancePayment(advance("concurrent-765", "100.00", null)))) {
                remoteCommitted = new CountDownLatch(1);
                releaseResponse = new CountDownLatch(1);
                holdResponse.set(true);
                Future<Object> first = workers.submit(() -> tx(operation));
                assertThat(remoteCommitted.await(10, TimeUnit.SECONDS)).isTrue();
                Future<Object> second = workers.submit(() -> tx(operation));
                try {
                    assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                            .as("duplicate source request waits until the first source transaction commits")
                            .isInstanceOf(TimeoutException.class);
                } finally { releaseResponse.countDown(); }
                assertThat(id(first.get(20, TimeUnit.SECONDS))).isEqualTo(id(second.get(20, TimeUnit.SECONDS)));
            }
            assertThat(journalCount()).isEqualTo(3);
            assertThat(remoteCalls.get()).isEqualTo(3);
            assertThat(count("receivable_source_creations")).isEqualTo(2);
            assertThat(count("payable_source_creations")).isOne();
        } finally { workers.shutdownNow(); }
    }

    @Test
    void preciseBoundaryAmountsSurviveRoundTripAndLossyAmountsFailBeforeRemote() {
        for (String amount : List.of("0.01", "99999999999999999.99")) {
            Collection saved = tx(() -> collections.receivePayment(collection("precise-" + amount, amount)));
            assertThat(saved.getAmount()).isEqualByComparingTo(amount);
            assertThat(tx(() -> collections.receivePayment(collection("precise-" + amount, amount + "0"))).getId()).isEqualTo(saved.getId());
        }
        int calls = remoteCalls.get();
        for (String amount : List.of("0.001", "100000000000000000.00")) {
            assertThatThrownBy(() -> collection("invalid", amount)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> invoice("invalid", amount, DATE)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> advance("invalid", amount, null)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(remoteCalls.get()).isEqualTo(calls);
    }

    private CollectionCommand collection(String key, String amount) {
        return new CollectionCommand(DATE, "C1", new BigDecimal(amount), "synthetic-bank", null, key);
    }
    private SalesInvoiceCommand invoice(String key, String amount, LocalDate due) {
        return new SalesInvoiceCommand(key, "C1", DATE, due, new BigDecimal(amount), BigDecimal.ONE,
                new BigDecimal(amount).subtract(BigDecimal.ONE), "SYSTEM", "synthetic invoice");
    }
    private AdvancePaymentCommand advance(String key, String amount, String description) {
        return new AdvancePaymentCommand("V1", DATE, new BigDecimal(amount), description, key);
    }
    private AdvancePayment sourceAdvance() {
        AdvancePayment a = new AdvancePayment(); a.setVendorCode("V1"); a.setPaymentDate(DATE);
        a.setAmount(new BigDecimal("200")); return save(a);
    }
    private Receivable sourceReceivable() {
        Receivable r = new Receivable(); r.setCustomerCode("C1"); r.setOriginalAmount(new BigDecimal("500"));
        r.setOutstandingAmount(new BigDecimal("500")); r.setStatus(ReceivableStatus.OPEN); return save(r);
    }
    private <T> T tx(Supplier<T> action) { return local.execute(status -> action.get()); }
    private void assertEmptySourceAndOneJournal() {
        assertThat(count("source_documents")).isZero();
        assertThat(count("receivable_source_creations")).isZero();
        assertThat(count("payable_source_creations")).isZero();
        assertOneJournal();
    }
    private int count(String table) { return source.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private int journalCount() { return journalJdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class); }
    private void assertOneJournal() {
        assertThat(journalCount()).isOne();
        assertThat(journalJdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isOne();
    }
    private Long id(Object object) {
        try { return (Long) object.getClass().getMethod("getId").invoke(object); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private <T> T save(T value) {
        try {
            Long id = id(value);
            if (id == null) {
                GeneratedKeyHolder key = new GeneratedKeyHolder();
                source.update(connection -> {
                    var insert = connection.prepareStatement("INSERT INTO source_documents(kind,payload) VALUES (?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    insert.setString(1, value.getClass().getSimpleName()); insert.setString(2, "{}"); return insert;
                }, key);
                id = key.getKey().longValue();
                allocatedIds.add(id);
                value.getClass().getMethod("setId", Long.class).invoke(value, id);
            }
            source.update("UPDATE source_documents SET payload=? WHERE id=?", json.writeValueAsString(value), id);
            return value;
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private <T> Optional<T> find(Class<T> type, Long id, boolean lock) {
        return source.query("SELECT payload FROM source_documents WHERE kind=? AND id=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> read(rs.getString(1), type), type.getSimpleName(), id).stream().findFirst();
    }
    private <T> List<T> all(Class<T> type) {
        return source.query("SELECT payload FROM source_documents WHERE kind=?",
                (rs, row) -> read(rs.getString(1), type), type.getSimpleName());
    }
    private <T> Optional<T> allocation(Long id, String key, Class<T> type) {
        return source.query("SELECT payload FROM source_allocations WHERE source_id=? AND operation_id=?",
                (rs, row) -> read(rs.getString(1), type), id, key).stream().findFirst();
    }
    private <T> T read(String value, Class<T> type) {
        try { return json.readValue(value, type); }
        catch (IOException exception) { throw new IllegalStateException(exception); }
    }
    private MockHttpServletResponse postJournal(String body) throws Exception {
        return http.perform(post("/api/v1/journals/posting")
                .header("X-Auth-User", "synthetic-maker").header("X-Auth-Roles", "ROLE_JOURNAL_MAKER")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.domain")
    @EnableJpaRepositories("com.ho.account.journalledger.infrastructure.persistence.repository")
    @Import({JournalPersistenceAdapter.class, JournalCreationOperationPersistenceAdapter.class})
    static class TestApplication {
        @Bean
        JournalEntryService journalEntryService(JournalPersistencePort journals,
                JournalCreationOperationPersistencePort operations) {
            JournalValidationEngine validation = new JournalValidationEngine(List.of(
                    new BalanceValidationFilter(), new ClosingLockValidationFilter(date -> false)));
            return new JournalEntryService(journals, operations, mock(JournalRuleEngine.class),
                    mock(PostingService.class), validation);
        }
    }

}
