package com.ho.account.closing.batch.adapter.out;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.closing.application.service.EclProvisionService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.ho.account.closing.batch.adapter.out.EclJournalFixture.DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EclProvisionCurrencyUnitsTest {
    private EclJournalFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new EclJournalFixture();
    }

    @ParameterizedTest
    @CsvSource({"80.00, 104000.00, 20.00, 26000.00", "0.00, 0.00, 100.00, 130000.00"})
    void usdTargetAndFunctionalGlBalanceKeepTheirOwnUnits(
            String existingTransaction, String existingBase, String expectedTransaction, String expectedBase) {
        fixture.target("USD", "100");
        fixture.rate("USD", "KRW", "1300");
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", existingTransaction, existingBase);
        // The legacy USD-labelled read model is a KRW total and must never be subtracted from USD100.
        fixture.jdbc.update("INSERT INTO gl_balances VALUES ('129100', 'USD', ?, '2026-05', ?)",
                DATE, new BigDecimal(existingBase).negate());

        fixture.service.processEclProvision(DATE, 781L);

        assertCommand(0, "USD", "1300", "550100", "129100", expectedTransaction, expectedBase);
        verify(fixture.rates).findLatestRateAt("USD", "KRW", DATE);
        verifyNoInteractions(fixture.eligibility);
    }

    @ParameterizedTest
    @CsvSource({
            "100, 20, 28000, 550100, 129100",
            "60, 20, 28000, 129100, 480100",
            "80, 0, 0, 550100, 129100",
            "0, 80, 112000, 129100, 480100"
    })
    void changedRateRequiresPostedFxValuationBeforeAdditionReleaseEqualityOrFullRelease(
            String target, String delta, String baseDelta, String debitAccount, String creditAccount) {
        fixture.target("USD", target);
        fixture.rate("USD", "KRW", "1400");
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "80", "104000");

        assertThatThrownBy(() -> fixture.service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("FX");
        verifyNoInteractions(fixture.posting, fixture.query);

        fixture.fx("POSTED", DATE, "KRW", "780|129100|USD", "129100", "CREDIT", "8000");
        fixture.service.processEclProvision(DATE, 781L);
        if ("0".equals(delta)) {
            assertThat(fixture.commands).isEmpty();
            verifyNoInteractions(fixture.posting, fixture.query);
        } else {
            assertCommand(0, "USD", "1400", debitAccount, creditAccount, delta, baseDelta);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "USD, EUR, 1.25, 80, 100, 20, 25",
            "EUR, USD, 1.1, 80, 88, 20, 22",
            "EUR, EUR, 1, 80, 80, 20, 20"
    })
    void configuredFunctionalCurrencyControlsRateAndBothUnits(
            String transaction, String functional, String rate, String existingTransaction, String existingBase,
            String delta, String baseDelta) {
        fixture.properties.setFxValuationReportingCurrencyCode(functional);
        fixture.target(transaction, "100");
        if (!transaction.equals(functional)) {
            fixture.rate(transaction, functional, rate);
        }
        fixture.ordinary("POSTED", DATE, transaction, "129100", "CREDIT", existingTransaction, existingBase);

        fixture.service.processEclProvision(DATE, 781L);

        assertCommand(0, transaction, rate, "550100", "129100", delta, baseDelta);
        if (transaction.equals(functional)) {
            verifyNoInteractions(fixture.rates);
        } else {
            verify(fixture.rates).findLatestRateAt(transaction, functional, DATE);
        }
    }

    @Test
    void actualDebitAllowanceBalanceIsReplenishedInBothCurrencies() {
        fixture.target("USD", "100");
        fixture.rate("USD", "KRW", "1300");
        fixture.ordinary("POSTED", DATE, "USD", "129100", "DEBIT", "20", "26000");

        fixture.service.processEclProvision(DATE, 781L);

        assertCommand(0, "USD", "1300", "550100", "129100", "120", "156000");
    }

    @Test
    void postedAdditionsAndReleasesReconcileBothUnitsAndRerunWithoutAnotherDraft() {
        fixture.properties.setAutoPostAdjustments(true);
        fixture.target("USD", "100");
        fixture.rate("USD", "KRW", "1300");
        fixture.service.processEclProvision(DATE, 781L);
        fixture.service.processEclProvision(DATE, 781L);
        assertBalance("100", "130000");
        assertThat(fixture.commands).hasSize(1);

        fixture.rate("USD", "KRW", "1400");
        fixture.replaceTarget("USD", "120");
        assertThatThrownBy(() -> fixture.service.processEclProvision(DATE, 782L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("FX");
        fixture.fx("POSTED", DATE, "KRW", "780|129100|USD", "129100", "CREDIT", "10000");
        fixture.service.processEclProvision(DATE, 782L);
        fixture.service.processEclProvision(DATE, 782L);
        assertBalance("120", "168000");
        assertCommand(1, "USD", "1400", "550100", "129100", "20", "28000");

        fixture.replaceTarget("USD", "80");
        fixture.service.processEclProvision(DATE, 783L);
        fixture.service.processEclProvision(DATE, 783L);
        assertBalance("80", "112000");
        assertCommand(2, "USD", "1400", "129100", "480100", "40", "56000");
        assertThat(fixture.commands).hasSize(3);
        assertThat(fixture.jdbc.queryForList(
                "SELECT exchange_rate FROM journal_entries WHERE lineage_source_type = 'ECL_PROVISION' ORDER BY id",
                BigDecimal.class)).containsExactly(new BigDecimal("1300.00000000"),
                new BigDecimal("1400.00000000"), new BigDecimal("1400.00000000"));
    }

    @ParameterizedTest
    @CsvSource({"0.01, 0.02, 0.02, 0.03, 550100, 129100", "0.02, 0.03, 0.01, 0.02, 129100, 480100"})
    void cumulativeBaseRoundingHitsTargetRatherThanRoundingDeltaAgain(
            String existing, String existingBase, String target, String targetBase,
            String debitAccount, String creditAccount) {
        fixture.properties.setAutoPostAdjustments(true);
        fixture.target("USD", target);
        fixture.rate("USD", "KRW", "1.5");
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", existing, existingBase);

        fixture.service.processEclProvision(DATE, 781L);
        fixture.service.processEclProvision(DATE, 781L);

        // round(target * rate) - existing base is .01; rounding delta * rate independently gives .02.
        assertCommand(0, "USD", "1.5", debitAccount, creditAccount, "0.01", "0.01");
        assertBalance(target, targetBase);
        assertThat(fixture.commands).hasSize(1);
    }

    @Test
    void positiveTransactionDeltaWithZeroBaseDeltaFailsBeforeRemoteWrites() {
        fixture.target("USD", "0.02");
        fixture.rate("USD", "KRW", "0.1");
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", "0.01", "0.00");

        assertThatThrownBy(() -> fixture.service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(fixture.posting, fixture.query);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100"})
    void missingRateFailsEvenIfTransactionTargetAlreadyEqualsBalance(String target) {
        fixture.target("USD", target);
        fixture.ordinary("POSTED", DATE, "USD", "129100", "CREDIT", target,
                new BigDecimal(target).multiply(new BigDecimal("1300")).toPlainString());

        assertThatThrownBy(() -> fixture.service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(fixture.posting, fixture.query);
    }

    @Test
    void laterInvalidGroupPreventsRemoteWritesForEarlierValidGroup() {
        fixture.target("EUR", "100");
        fixture.rate("EUR", "KRW", "1500");
        fixture.target("USD", "100");
        // USD sorts after EUR and has no rate. No EUR draft may escape the complete preflight.
        assertThatThrownBy(() -> fixture.service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(fixture.posting, fixture.query);
    }

    @ParameterizedTest
    @CsvSource({
            "run_id, RUN-OTHER, one finalized run/model",
            "model_version, MODEL-OTHER, one finalized run/model",
            "legal_entity_code, OTHER, exactly one legal entity"
    })
    void distinctJdbcSnapshotIdentitiesFailBeforeBalanceRateOrJournalAccess(
            String identityColumn, String conflictingValue, String expectedMessage) {
        fixture.target("USD", "100");
        fixture.jdbc.update("""
                INSERT INTO allowance_summary
                SELECT base_date, run_id, model_version, legal_entity_code, currency_code,
                       '12100', allowance_account_code, bad_debt_expense_account_code,
                       reversal_income_account_code, target_allowance_amount, source_exposure_amount,
                       stage1_allowance_amount, stage2_allowance_amount, stage3_allowance_amount
                FROM allowance_summary
                """);
        // Each column comes only from this closed test case list; preserve distinct JDBC grouping identities.
        fixture.jdbc.update("UPDATE allowance_summary SET " + identityColumn
                + " = ? WHERE exposure_account_code = '12100'", conflictingValue);
        var observedBalances = spy(fixture.balances);
        var service = new EclProvisionService(observedBalances,
                new JournalLedgerClosingJournalEntryAdapter(fixture.posting, fixture.query), fixture.properties,
                new JdbcEclAllowanceResultAdapter(fixture.jdbc),
                new MasterDataFxExchangeRateLookupAdapter(fixture.rates));

        assertThatThrownBy(() -> service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(expectedMessage);

        verifyNoInteractions(observedBalances, fixture.rates, fixture.query, fixture.posting);
    }

    @Test
    void normalizedSameRateReusesDraftButChangedRateWithSameRoundedAmountsFails() {
        fixture.target("USD", "0.01");
        fixture.rate("USD", "KRW", "1.1000");
        fixture.service.processEclProvision(DATE, 781L);
        fixture.rate("USD", "KRW", "1.1");
        fixture.service.processEclProvision(DATE, 781L);
        assertThat(fixture.commands).hasSize(1);
        assertThat(fixture.commands.get(0).description()).contains("[USD/KRW @ 1.1]");

        fixture.rate("USD", "KRW", "1.2");
        assertThatThrownBy(() -> fixture.service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different business content");
        verify(fixture.posting, times(1)).createDraftEntry(any());
    }

    @Test
    void committedForeignDraftWithLostResponseCanBeReusedAtStoredPrecision() {
        fixture.target("USD", "100.0050");
        fixture.rate("USD", "KRW", "1300.12345678");
        doAnswer(invocation -> {
            fixture.persist(invocation.getArgument(0));
            throw new IllegalStateException("Remote draft committed but response was lost");
        }).when(fixture.posting).createDraftEntry(any());
        assertThatThrownBy(() -> fixture.service.processEclProvision(DATE, 781L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("response was lost");

        fixture.service.processEclProvision(DATE, 781L);

        assertCommand(0, "USD", "1300.12345678", "550100", "129100", "100.01", "130025.35");
        verify(fixture.posting, times(1)).createDraftEntry(any());
    }

    private void assertBalance(String transaction, String base) {
        var balance = fixture.balances.findCreditBalance("129100", "USD", "KRW", DATE);
        assertThat(balance.creditTransactionAmount()).isEqualByComparingTo(transaction);
        assertThat(balance.creditBaseAmount()).isEqualByComparingTo(base);
    }

    private void assertCommand(int index, String currency, String rate, String debitAccount,
                               String creditAccount, String transaction, String base) {
        JournalEntryCommand command = fixture.commands.get(index);
        assertThat(command.currencyCode()).isEqualTo(currency);
        assertThat(command.exchangeRate()).isEqualByComparingTo(rate);
        assertThat(command.accountingDate()).isEqualTo(DATE);
        assertThat(command.lineageSourceType()).isEqualTo("ECL_PROVISION");
        assertThat(command.lines()).hasSize(2).allSatisfy(line -> {
            assertThat(line.amount()).isEqualByComparingTo(transaction);
            assertThat(line.baseAmount()).isEqualByComparingTo(base);
            assertThat(line.amount().scale()).isEqualTo(2);
            assertThat(line.baseAmount().scale()).isEqualTo(2);
        });
        assertThat(command.lines().get(0).accountCode()).isEqualTo(debitAccount);
        assertThat(command.lines().get(0).drcrType()).isEqualTo("DEBIT");
        assertThat(command.lines().get(1).accountCode()).isEqualTo(creditAccount);
        assertThat(command.lines().get(1).drcrType()).isEqualTo("CREDIT");
    }
}
