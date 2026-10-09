package com.ho.account.closing.config;

import com.ho.account.closing.application.port.in.FinancialClosingCalculation;
import com.ho.account.closing.application.port.in.FinancialClosingCalculationResult;
import com.ho.account.closing.application.port.out.ClosingFinancialRunManifestPort;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.EclProvisionService;
import com.ho.account.closing.application.service.FinancialClosingCalculationService;
import com.ho.account.closing.application.service.FxValuationEligibilityResolver;
import com.ho.account.closing.application.service.FxValuationService;
import com.ho.account.closing.infrastructure.external.JournalLedgerClosingJournalEntryAdapter;
import com.ho.account.closing.infrastructure.external.MasterDataFxExchangeRateLookupAdapter;
import com.ho.account.closing.infrastructure.source.ClosingReadOnlySources;
import com.ho.account.closing.infrastructure.source.JdbcClosingMasterDataAdapter;
import com.ho.account.closing.infrastructure.source.JdbcEclAllowanceResultAdapter;
import com.ho.account.closing.infrastructure.source.JdbcPostedJournalFinancialEvidenceAdapter;
import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import com.ho.account.contracts.masterdata.ExchangeRateQueryPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * API composition root for synchronous, evidence-backed financial closing runs.
 *
 * <p>Only the core use case is published as a bean. Source-specific adapters remain private to
 * this composition so the API cannot accidentally select a foreign data source by type.</p>
 */
@Configuration(proxyBeanMethods = false)
public class FinancialClosingConfiguration {

    private static final String FX_SOURCE_UNAVAILABLE =
            "FX valuation source evidence is unavailable because closing.sources.enabled is false";
    private static final String ECL_SOURCE_UNAVAILABLE =
            "ECL allowance source evidence is unavailable because closing.sources.enabled is false";
    private static final String JOURNAL_UNAVAILABLE =
            "Journal Ledger is unavailable because closing.sources.enabled is false";

    @Bean
    @Profile("!dev")
    FinancialClosingCalculation monolithFinancialClosingCalculation(
            JdbcTemplate jdbcTemplate,
            JournalPostingPort journalPostingPort,
            JournalQueryPort journalQueryPort,
            MasterDataQueryPort masterDataQueryPort,
            ExchangeRateQueryPort exchangeRateQueryPort,
            ClosingAccountingProperties accountingProperties,
            ClosingFinancialRunManifestPort commandManifest) {
        FxExchangeRateLookupPort exchangeRateLookupPort =
                new MasterDataFxExchangeRateLookupAdapter(exchangeRateQueryPort);
        return compose(
                jdbcTemplate,
                jdbcTemplate,
                masterDataQueryPort,
                exchangeRateLookupPort,
                journalPostingPort,
                journalQueryPort,
                accountingProperties,
                commandManifest);
    }

    @Bean
    @Profile("dev")
    @ConditionalOnProperty(name = "closing.sources.enabled", havingValue = "true")
    FinancialClosingCalculation devFinancialClosingCalculation(
            ClosingReadOnlySources sources,
            JournalPostingPort journalPostingPort,
            JournalQueryPort journalQueryPort,
            ClosingAccountingProperties accountingProperties,
            ClosingFinancialRunManifestPort commandManifest) {
        JdbcClosingMasterDataAdapter masterDataAdapter =
                new JdbcClosingMasterDataAdapter(sources.masterDataJdbcTemplate());
        return compose(
                sources.journalJdbcTemplate(),
                sources.eclJdbcTemplate(),
                masterDataAdapter,
                masterDataAdapter,
                journalPostingPort,
                journalQueryPort,
                accountingProperties,
                commandManifest);
    }

    @Bean
    @Profile("dev")
    @ConditionalOnProperty(
            name = "closing.sources.enabled",
            havingValue = "false",
            matchIfMissing = true)
    FinancialClosingCalculation unavailableFinancialClosingCalculation() {
        return new FinancialClosingCalculation() {
            @Override
            public FinancialClosingCalculationResult runFxValuation(
                    LocalDate valuationDate,
                    Long valuationBatchId) {
                throw new IllegalStateException(FX_SOURCE_UNAVAILABLE);
            }

            @Override
            public FinancialClosingCalculationResult runEclProvision(
                    LocalDate closingDate,
                    Long provisionBatchId) {
                throw new IllegalStateException(ECL_SOURCE_UNAVAILABLE);
            }

            @Override
            public void validateFxValuation(LocalDate valuationDate, Long valuationBatchId) {
                throw new IllegalStateException(FX_SOURCE_UNAVAILABLE);
            }
        };
    }

    /** Keeps the dev API context available without constructing a remote Journal client. */
    @Bean
    @Profile("dev")
    @ConditionalOnProperty(
            name = "closing.sources.enabled",
            havingValue = "false",
            matchIfMissing = true)
    UnavailableJournalPorts unavailableJournalPorts() {
        return new UnavailableJournalPorts();
    }

    private static FinancialClosingCalculation compose(
            JdbcTemplate journalJdbcTemplate,
            JdbcTemplate eclJdbcTemplate,
            MasterDataQueryPort masterDataQueryPort,
            FxExchangeRateLookupPort exchangeRateLookupPort,
            JournalPostingPort journalPostingPort,
            JournalQueryPort journalQueryPort,
            ClosingAccountingProperties accountingProperties,
            ClosingFinancialRunManifestPort commandManifest) {
        JdbcPostedJournalFinancialEvidenceAdapter financialEvidence =
                new JdbcPostedJournalFinancialEvidenceAdapter(
                        journalJdbcTemplate,
                        JdbcPostedJournalFinancialEvidenceAdapter.DEFAULT_QUERY_TIMEOUT_SECONDS,
                        accountingProperties.getApiFinancialRunMaxEvidenceRows());
        JournalLedgerClosingJournalEntryAdapter journalEntryAdapter =
                new JournalLedgerClosingJournalEntryAdapter(journalPostingPort, journalQueryPort);
        FxValuationService fxValuationService = new FxValuationService(
                exchangeRateLookupPort,
                journalEntryAdapter,
                accountingProperties,
                new FxValuationEligibilityResolver(accountingProperties, masterDataQueryPort));
        EclProvisionService eclProvisionService = new EclProvisionService(
                financialEvidence,
                journalEntryAdapter,
                accountingProperties,
                new JdbcEclAllowanceResultAdapter(
                        eclJdbcTemplate,
                        accountingProperties.getApiFinancialRunMaxJournalCommands(),
                        JdbcEclAllowanceResultAdapter.DEFAULT_QUERY_TIMEOUT_SECONDS),
                exchangeRateLookupPort);
        return new FinancialClosingCalculationService(
                financialEvidence,
                fxValuationService,
                eclProvisionService,
                accountingProperties,
                commandManifest);
    }

    static final class UnavailableJournalPorts implements JournalPostingPort, JournalQueryPort {

        @Override
        public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
            throw unavailable();
        }

        @Override
        public void approveAndPost(Long journalEntryId, String actor) {
            throw unavailable();
        }

        @Override
        public List<JournalSummary> getJournalSummaries(LocalDate startDate, LocalDate endDate) {
            throw unavailable();
        }

        @Override
        public List<JournalDetailSummary> getJournalDetails(Long journalEntryId) {
            throw unavailable();
        }

        @Override
        public List<JournalDetailSummary> getJournalDetailsByAccountCodes(
                LocalDate startDate,
                LocalDate endDate,
                List<String> accountCodes) {
            throw unavailable();
        }

        @Override
        public JournalDetailAggregateSummary getJournalDetailAggregate(
                LocalDate startDate,
                LocalDate endDate,
                JournalSide side) {
            throw unavailable();
        }

        @Override
        public JournalDetailAggregateSummary getJournalDetailAggregateByAccount(
                LocalDate startDate,
                LocalDate endDate,
                JournalSide side,
                String accountCode) {
            throw unavailable();
        }

        @Override
        public JournalSummary getJournalSummary(Long journalEntryId) {
            throw unavailable();
        }

        @Override
        public Optional<JournalSummary> findBySlipNo(String slipNo) {
            throw unavailable();
        }

        private IllegalStateException unavailable() {
            return new IllegalStateException(JOURNAL_UNAVAILABLE);
        }
    }
}
