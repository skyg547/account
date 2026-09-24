package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.closing.domain.fx.FxValuationPolicy;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FxValuationEligibilityServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 5, 31);
    private final MasterDataQueryPort metadata = mock(MasterDataQueryPort.class);
    private final FxExchangeRateLookupPort rates = mock(FxExchangeRateLookupPort.class);
    private final ClosingJournalEntryPort journals = mock(ClosingJournalEntryPort.class);
    private final ClosingAccountingProperties properties = new ClosingAccountingProperties();
    private FxValuationService service;

    @BeforeEach
    void setUp() {
        properties.setFxValuationReportingCurrencyCode("USD");
        service = new FxValuationService(rates, journals, properties,
                new FxValuationEligibilityResolver(properties, metadata));
    }

    @ParameterizedTest
    @CsvSource({"REVENUE,false,CREDIT,100,110,DEBIT,72000", "EXPENSES,false,DEBIT,-100,-110,CREDIT,92000",
            "ASSETS,true,DEBIT,-100,-110,CREDIT,92000", "ASSETS,false,DEBIT,-100,-110,CREDIT,92000"})
    void completeJournalExcludesHistoricalLegAndPreservesMonetaryGainOrLoss(
            String historicalCategory, boolean fixedAsset, String historicalSide,
            String foreign, String book, ClosingJournalSide adjustmentSide, String pnlAccount) {
        String monetaryCategory = historicalSide.equals("CREDIT") ? "ASSETS" : "LIABILITIES";
        configure("MONETARY", monetaryCategory, false, FxValuationPolicy.Treatment.MONETARY);
        configure("HISTORICAL", historicalCategory, fixedAsset, FxValuationPolicy.Treatment.HISTORICAL_COST);
        when(rates.findRate("EUR", "USD", DATE)).thenReturn(Optional.of(new BigDecimal("1.2")));
        when(journals.createDraftAdjustment(any())).thenReturn(new ClosingJournalEntryResult(1L, "FX"));

        service.processFxValuationForAccount(balance("MONETARY", foreign, book), DATE, 780L);
        service.processFxValuationForAccount(balance("HISTORICAL", new BigDecimal(foreign).negate().toPlainString(),
                new BigDecimal(book).negate().toPlainString()), DATE, 780L);

        var captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(journals).createDraftAdjustment(captor.capture());
        assertThat(captor.getValue().lines().get(0).accountCode()).isEqualTo("MONETARY");
        assertThat(captor.getValue().lines().get(0).side()).isEqualTo(adjustmentSide);
        assertThat(captor.getValue().lines().get(0).amount()).isEqualByComparingTo("10");
        assertThat(captor.getValue().lines().get(1).accountCode()).isEqualTo(pnlAccount);
        verify(rates).findRate("EUR", "USD", DATE);
        verify(journals, never()).approveAndPost(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"ASSETS,DEBIT,100,110,DEBIT", "ASSETS,DEBIT,-100,-110,CREDIT",
            "LIABILITIES,CREDIT,-100,-110,CREDIT", "LIABILITIES,CREDIT,100,110,DEBIT"})
    void actualSignedBalanceControlsNormalAndAbnormalMonetaryAdjustments(
            String category, String normalSide, String foreign, String book, ClosingJournalSide expected) {
        configure("A", category, false, FxValuationPolicy.Treatment.MONETARY);
        when(metadata.findAccountSubjectAt("A", DATE)).thenReturn(Optional.of(
                new AccountSubjectRef("A", "Account", false, false, normalSide, category)));
        when(rates.findRate("EUR", "USD", DATE)).thenReturn(Optional.of(new BigDecimal("1.2")));
        when(journals.createDraftAdjustment(any())).thenReturn(new ClosingJournalEntryResult(1L, "FX"));

        service.processFxValuationForAccount(balance("A", foreign, book), DATE, 780L);

        var captor = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(journals).createDraftAdjustment(captor.capture());
        assertThat(captor.getValue().lines().get(0).side()).isEqualTo(expected);
        assertThat(captor.getValue().lines().get(0).amount()).isEqualByComparingTo("10");
    }

    @ParameterizedTest
    @CsvSource({"0,0", "100,120"})
    void missingPolicyCannotHideBehindZeroBalancesOrUnchangedValuation(String foreign, String book) {
        when(metadata.findAccountSubjectAt("A", DATE)).thenReturn(Optional.of(
                new AccountSubjectRef("A", "Cash", false, false, "DEBIT", "ASSETS")));
        assertThatThrownBy(() -> service.processFxValuationForAccount(balance("A", foreign, book), DATE, 780L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("policy is missing");
        verifyNoInteractions(rates, journals);
    }

    @Test
    void missingDatedMetadataCannotHideBehindUnchangedValuation() {
        assertThatThrownBy(() -> service.processFxValuationForAccount(balance("A", "100", "120"), DATE, 780L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Account subject is missing");
        verifyNoInteractions(rates, journals);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVENUE", "EXPENSES", "EQUITY", "UNKNOWN"})
    void directServiceRejectsContradictoryOrUnknownMetadataBeforeRateLookup(String category) {
        configure("A", category, false, FxValuationPolicy.Treatment.MONETARY);
        assertThatThrownBy(() -> service.processFxValuationForAccount(balance("A", "100", "110"), DATE, 780L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(rates, journals);
    }

    @Test
    void historicalFixedAssetAndPrepaymentNeedNoRateAndProduceNoJournal() {
        for (boolean fixed : List.of(true, false)) {
            configure("A", "ASSETS", fixed, FxValuationPolicy.Treatment.HISTORICAL_COST);
            service.processFxValuationForAccount(balance("A", "100", "110"), DATE, 780L);
            properties.setFxValuationPolicies(List.of());
        }
        verifyNoInteractions(rates, journals);
    }

    @Test
    void metadataIdentityAndDateCannotBeSubstituted() {
        configure("A", "ASSETS", false, FxValuationPolicy.Treatment.MONETARY);
        when(metadata.findAccountSubjectAt("A", DATE)).thenReturn(Optional.of(
                new AccountSubjectRef("OTHER", "Wrong account", false, false, "DEBIT", "ASSETS")));
        assertThatThrownBy(() -> service.processFxValuationForAccount(balance("A", "100", "110"), DATE, 780L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("does not match");
        verify(metadata).findAccountSubjectAt("A", DATE);
        verify(metadata, never()).findAccountSubject(any());
        verifyNoInteractions(rates, journals);
    }

    @Test
    void configurationBindingBuildsValidatedDatedPolicy() {
        String prefix = "account.closing.accounting.fx-valuation-policies[0].";
        var source = new MapConfigurationPropertySource(Map.of(
                prefix + "account-code", "A", prefix + "effective-from", "2026-01-01",
                prefix + "effective-to", "2026-12-31", prefix + "treatment", "MONETARY"));
        new Binder(source).bind("account.closing.accounting", Bindable.ofInstance(properties));
        assertThat(properties.getFxValuationPolicies()).hasSize(1);
        assertThat(properties.fxValuationPolicy().isEligible("A", DATE, "ASSETS", false)).isTrue();
        assertThatThrownBy(() -> properties.getFxValuationPolicies().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private void configure(String account, String category, boolean fixed, FxValuationPolicy.Treatment treatment) {
        var rules = new java.util.ArrayList<>(properties.getFxValuationPolicies());
        rules.add(new FxValuationPolicy.Rule(account, DATE, DATE, treatment));
        properties.setFxValuationPolicies(rules);
        when(metadata.findAccountSubjectAt(account, DATE)).thenReturn(Optional.of(
                new AccountSubjectRef(account, "Account", false, fixed, "DEBIT", category)));
    }

    private FxValuationBalance balance(String account, String foreign, String book) {
        return new FxValuationBalance(account, "EUR", new BigDecimal(foreign), new BigDecimal(book));
    }
}
