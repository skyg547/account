package com.ho.account.asset.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.asset.application.port.out.*;
import com.ho.account.asset.domain.*;
import com.ho.account.asset.infrastructure.adapter.AssetSourceDocumentProvider;
import com.ho.account.asset.infrastructure.persistence.*;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.out.*;
import com.ho.account.journalledger.application.service.journal.*;
import com.ho.account.journalledger.application.service.journal.validator.*;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.*;
import com.ho.account.journalledger.infrastructure.persistence.JournalPersistenceAdapter;
import com.ho.account.journalledger.infrastructure.persistence.JournalCreationOperationPersistenceAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;

/** Actual producer payloads cross JSON transport into the real rule engine and durable Journal service.
 * This intentionally does not model Kafka/source-DB atomicity: producer and consumer commits are separate.
 */
@SpringBootTest(classes = AssetJournalProducerPersistenceTest.App.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.config.name=asset-journal-producer-test", "spring.datasource.url=" + AssetJournalProducerPersistenceTest.URL,
                "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.mapping-resources=META-INF/journal-ledger-orm.xml",
                "spring.flyway.enabled=true", "spring.flyway.baseline-on-migrate=true", "spring.flyway.baseline-version=0", "spring.flyway.locations=classpath:db/journal-migration",
                "spring.sql.init.mode=never", "spring.cloud.config.enabled=false", "spring.cloud.vault.enabled=false",
                "spring.cloud.discovery.enabled=false", "eureka.client.enabled=false"})
@ContextConfiguration(initializers = AssetJournalProducerPersistenceTest.SourceSchema.class)
class AssetJournalProducerPersistenceTest {
    static final String URL = "jdbc:h2:mem:asset-journal-955;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    @Autowired FixedAssetEntryService assets;
    @Autowired LeaseEntryService leases;
    @Autowired JournalEntryService journals;
    @Autowired RecordingEvents events;
    @Autowired JdbcTemplate jdbc;
    @Autowired AssetSourceDocumentProvider sources;
    @Autowired LeasePersistencePort leasePersistence;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    @BeforeEach
    void clear() {
        events.payloads.clear();
        for (String table : List.of("journal_creation_operations", "journal_details", "journal_entries",
                "lease_remeasurement_receipts", "lease_payment_schedules", "lease_liabilities", "right_of_use_assets",
                "lease_contracts", "asset_histories", "fixed_assets")) jdbc.update("DELETE FROM " + table);
    }

    @Test
    void acquisitionMonthlyAndDisposalPayloadsPersistDistinctOperationsAndRejectChangedContent() throws Exception {
        FixedAsset asset = asset("FA-955");
        assets.registerAsset(asset, "asset-maker");
        Map<String, Object> acquisition = events.last();
        assertThat(acquisition).containsEntry("assetCode", "FA-955").containsEntry("accountingDate", "2026-01-01");
        assertRetryAndConflict(acquisition, "amount");
        assertThat(sources.getSourceDocument("FIXED_ASSET_CODE", "FA-955")).isPresent();
        assertThat(sources.getSourceDocument("FIXED_ASSET", asset.getId().toString())).isPresent();
        assets.processMonthlyDepreciation(LocalDate.of(2026, 1, 31), "asset-maker");
        Map<String, Object> january = events.last();
        assertRetryAndConflict(january, "amount");
        int count = events.payloads.size();
        assets.processMonthlyDepreciation(LocalDate.of(2026, 1, 31), "asset-maker");
        assertThat(events.payloads).hasSize(count);
        assets.processMonthlyDepreciation(LocalDate.of(2026, 2, 28), "asset-maker");
        Map<String, Object> february = events.last();
        assertThat(JournalCreationIdentity.fromEvent(february)).isNotEqualTo(JournalCreationIdentity.fromEvent(january));
        assertRetryAndConflict(february, "amount");
        assets.disposeFixedAsset(asset.getId(), LocalDate.of(2026, 3, 1), new BigDecimal("2000.00"), "asset-maker");
        assertRetryAndConflict(events.last(), "salePrice");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isEqualTo(4);
    }

    @Test
    void initialMonthlyAndRemeasurementPreserveOneToManyEventsAndOriginalCommittedRetry() throws Exception {
        LeaseContract lease = leases.registerLeaseContract(lease("LEASE-955"), "lease-maker");
        Map<String, Object> initial = events.last();
        assertThat(JournalCreationIdentity.fromEvent(initial).sourceId()).isEqualTo("LEASE-955");
        assertThat(initial).containsEntry("contractNo", lease.getContractNo()).doesNotContainKey("contractId");
        assertRetryAndConflict(initial, "rouAmount");
        assertThat(sources.getSourceDocument("IFRS16_LEASE_CONTRACT_NO", "LEASE-955")).isPresent();
        assertThat(sources.getSourceDocument("IFRS16_LEASE", lease.getId().toString())).isPresent();
        leases.processMonthlyLeaseAccounting(LocalDate.of(2026, 1, 31), "lease-maker");
        Map<String, Object> january = events.last();
        assertRetryAndConflict(january, "interestAmount");
        int count = events.payloads.size();
        leases.processMonthlyLeaseAccounting(LocalDate.of(2026, 1, 31), "lease-maker");
        assertThat(events.payloads).hasSize(count);
        leases.processMonthlyLeaseAccounting(LocalDate.of(2026, 2, 28), "lease-maker");
        assertThat(JournalCreationIdentity.fromEvent(events.last())).isNotEqualTo(JournalCreationIdentity.fromEvent(january));
        assertRetryAndConflict(events.last(), "interestAmount");
        LocalDate date = LocalDate.of(2026, 3, 1);
        leases.remeasureLease(lease.getId(), date, new BigDecimal("2400.00"), null, null, "lease-maker", "AMENDMENT-1");
        Map<String, Object> first = new HashMap<>(events.last());
        Long originalJournal = assertRetryAndConflict(first, "adjustmentAmount");
        leases.remeasureLease(lease.getId(), date, new BigDecimal("2400.0"), null, null, "lease-maker", "AMENDMENT-1");
        assertThat(events.last()).isEqualTo(first);
        assertThat(consume(events.last()).getId()).isEqualTo(originalJournal);
        BigDecimal balance = leasePersistence.findLiabilityByContract(lease).orElseThrow().getCurrentValue();
        assertThatThrownBy(() -> leases.remeasureLease(lease.getId(), date, new BigDecimal("2500.00"), null, null,
                "lease-maker", "AMENDMENT-1")).isInstanceOf(LeaseRemeasurementConflictException.class);
        assertThat(leasePersistence.findLiabilityByContract(lease).orElseThrow().getCurrentValue()).isEqualByComparingTo(balance);
        leases.remeasureLease(lease.getId(), date, new BigDecimal("2500.00"), null, null, "lease-maker", "AMENDMENT-2");
        assertThat(JournalCreationIdentity.fromEvent(events.last())).isNotEqualTo(JournalCreationIdentity.fromEvent(first));
        assertRetryAndConflict(events.last(), "adjustmentAmount");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lease_remeasurement_receipts", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isEqualTo(5);
    }

    @Test
    void initialRecognitionRetryAfterSourceRollbackAndNewDatabaseIdReusesJournal() throws Exception {
        org.springframework.transaction.support.TransactionTemplate tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        LeaseContract rolledBack = tx.execute(status -> {
            LeaseContract created = leases.registerLeaseContract(lease("ROLLBACK-LEASE"), "lease-maker");
            status.setRollbackOnly();
            return created;
        });
        Map<String, Object> original = new HashMap<>(events.last());
        Long journalId = consume(original).getId();
        LeaseContract recreated = leases.registerLeaseContract(lease("ROLLBACK-LEASE"), "lease-maker");
        assertThat(recreated.getId()).isNotEqualTo(rolledBack.getId());
        assertThat(events.last()).isEqualTo(original);
        assertThat(consume(events.last()).getId()).isEqualTo(journalId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isOne();
    }

    @Test
    void sourceRollbackChangedRawNullableTermsConflictEvenWhenFinancialResultIsEqual() throws Exception {
        LeaseContract lease = leases.registerLeaseContract(lease("RAW-ROLLBACK"), "lease-maker");
        LocalDate date = LocalDate.of(2026, 1, 1);
        org.springframework.transaction.support.TransactionTemplate tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            leases.remeasureLease(lease.getId(), date, new BigDecimal("2400.00"), null, null, "lease-maker", "RAW-1");
            status.setRollbackOnly();
        });
        Map<String, Object> original = new HashMap<>(events.last());
        assertThat(leasePersistence.findRemeasurementReceipt(lease.getContractNo(), "RAW-1")).isEmpty();
        Long firstJournal = consume(original).getId();
        // An explicit rate is a different submitted request from null, despite the same effective rate.
        leases.remeasureLease(lease.getId(), date, new BigDecimal("2400.00"), null, new BigDecimal("6.0"), "lease-maker", "RAW-1");
        Map<String, Object> changed = events.last();
        assertThat(changed.get("adjustmentAmount")).isEqualTo(original.get("adjustmentAmount"));
        assertThat(changed.get("newLiabilityAmount")).isEqualTo(original.get("newLiabilityAmount"));
        assertThat(changed.get("sourceRequestFingerprint")).isNotEqualTo(original.get("sourceRequestFingerprint"));
        assertThatThrownBy(() -> consume(changed)).isInstanceOf(JournalCreationConflictException.class);
        assertThat(consume(original).getId()).isEqualTo(firstJournal);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isOne();
    }

    private Long assertRetryAndConflict(Map<String, Object> event, String financialField) throws Exception {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class);
        JournalEntry first = consume(event);
        assertThat(first.getAccountingDate()).isEqualTo(LocalDate.parse((String) event.get("accountingDate")));
        assertThat(consume(event).getId()).isEqualTo(first.getId());
        Map<String, Object> changed = new HashMap<>(event);
        changed.put(financialField, new BigDecimal(event.get(financialField).toString()).add(BigDecimal.ONE));
        assertThatThrownBy(() -> consume(changed)).isInstanceOf(JournalCreationConflictException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isEqualTo(before + 1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(before + 1);
        return first.getId();
    }

    private JournalEntry consume(Map<String, Object> payload) throws Exception {
        ObjectMapper mapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        Map<String, Object> decoded = mapper.readValue(mapper.writeValueAsString(payload), new TypeReference<>() {});
        return journals.createJournalEntryFromEvent(JournalCreationIdentity.fromEvent(decoded), decoded,
                LocalDate.parse((String) decoded.get("accountingDate"))).orElseThrow();
    }

    static FixedAsset asset(String code) {
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode(code); asset.setAssetName("Synthetic asset"); asset.setAccountCode("15000");
        asset.setDepartmentCode("D955"); asset.setAcquisitionDate(LocalDate.of(2026, 1, 1));
        asset.setAcquisitionCost(new BigDecimal("12000.00")); asset.setUsefulLife(12);
        asset.setDepreciationAmountPerPeriod(new BigDecimal("1000.00"));
        return asset;
    }

    static LeaseContract lease(String number) {
        LeaseContract lease = new LeaseContract();
        lease.setContractNo(number); lease.setContractName("Synthetic lease"); lease.setDepartmentCode("D955");
        lease.setStartDate(LocalDate.of(2026, 1, 1)); lease.setEndDate(LocalDate.of(2026, 12, 31));
        lease.setMonthlyPayment(new BigDecimal("1200.00")); lease.setDiscountRate(new BigDecimal("6.0"));
        lease.setPaymentDay(25); lease.setStatus("ACTIVE");
        return lease;
    }

    static class RecordingEvents implements AssetEventPort {
        final List<Map<String, Object>> payloads = new ArrayList<>();
        public void sendAssetEvent(String topic, Map<String, Object> payload) { payloads.add(new HashMap<>(payload)); }
        Map<String, Object> last() { return payloads.get(payloads.size() - 1); }
    }

    public static class SourceSchema implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext context) {
            new ResourceDatabasePopulator(new ClassPathResource("db/postgresql-migration/V20__init_asset_lease.sql"),
                    new ClassPathResource("db/postgresql-migration/V21__lease_remeasurement_receipts.sql"))
                    .execute(new DriverManagerDataSource(URL, "sa", ""));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan({"com.ho.account.asset.domain", "com.ho.account.asset.infrastructure.persistence.entity", "com.ho.account.journalledger.domain"})
    @EnableJpaRepositories({"com.ho.account.asset.infrastructure.persistence.repository", "com.ho.account.journalledger.infrastructure.persistence.repository"})
    @Import({FixedAssetEntryService.class, LeaseEntryService.class, FixedAssetPersistenceAdapter.class,
            LeasePersistenceAdapter.class, AssetSourceDocumentProvider.class,
            JournalPersistenceAdapter.class, JournalCreationOperationPersistenceAdapter.class})
    static class App {
        @Bean RecordingEvents events() { return new RecordingEvents(); }
        @Bean LeasePaymentResolutionPort payments() { return command -> {}; }
        @Bean LeaseAccountMappingPort accounts() { return contract -> new LeaseAccountMappingPort.LeasePaymentAccounts("25100", "93100", "21100"); }
        @Bean JournalEntryService journals(JournalPersistencePort journals, JournalCreationOperationPersistencePort operations) {
            JournalRuleQueryPort query = mock(JournalRuleQueryPort.class);
            List<JournalRule> rules = new ArrayList<>();
            String[] types = {"ASSET_ACQUISITION", "ASSET_DEPRECIATION", "ASSET_DISPOSAL",
                    "IFRS16_INITIAL_RECOGNITION", "IFRS16_MONTHLY_PROCESS", "IFRS16_REMEASUREMENT"};
            String[] amounts = {"amount", "amount", "salePrice", "rouAmount", "depreciationAmount", "adjustmentAmount"};
            for (int i = 0; i < types.length; i++) {
                long id = i + 1;
                JournalRule rule = new JournalRule(); rule.setId(id); rule.setRuleCode(types[i]);
                rule.setRuleName(types[i]); rule.setActive(true); rule.setPriority(i); rules.add(rule);
                JournalRuleCondition condition = new JournalRuleCondition(); condition.setField("transactionType");
                condition.setOperator(ConditionOperator.EQUALS); condition.setValue(types[i]);
                when(query.findConditions(id)).thenReturn(List.of(condition));
                List<JournalRuleDetail> lines = new ArrayList<>();
                for (String side : List.of("DEBIT", "CREDIT")) {
                    JournalRuleDetail line = new JournalRuleDetail(); line.setId(id * 10 + lines.size());
                    line.setDrcrType(side); line.setAccountSubjectCodeExpression(side.equals("DEBIT") ? "15000" : "25100");
                    line.setAmountExpression("${" + amounts[i] + "}"); line.setDepartmentCodeExpression("${deptCode}"); lines.add(line);
                }
                when(query.findDetails(id)).thenReturn(lines);
            }
            when(query.findActiveRules()).thenReturn(rules);
            return new JournalEntryService(journals, operations, new JournalRuleEngine(query), mock(PostingService.class),
                    new JournalValidationEngine(List.of(new BalanceValidationFilter(), new ClosingLockValidationFilter(date -> false))));
        }
    }
}
