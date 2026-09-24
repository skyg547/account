package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.*;
import com.ho.account.closing.domain.fx.FxValuationPolicy;
import java.util.List;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FxValuationEligibilityRegressionTest {
    @Test
    void completeCashRevenueJournalMustProduceOnlyCashGain() {
        var date = LocalDate.of(2026, 5, 31);
        var metadata = mock(MasterDataQueryPort.class);
        when(metadata.findAccountSubjectAt("11000", date)).thenReturn(Optional.of(
                new AccountSubjectRef("11000", "Cash", false, false, "DEBIT", "ASSET")));
        when(metadata.findAccountSubjectAt("41000", date)).thenReturn(Optional.of(
                new AccountSubjectRef("41000", "Revenue", false, false, "CREDIT", "REVENUE")));
        var journals = mock(ClosingJournalEntryPort.class);
        when(journals.createDraftAdjustment(any())).thenReturn(new ClosingJournalEntryResult(1L, "FX-REGRESSION"));
        var properties = new ClosingAccountingProperties();
        properties.setFxValuationReportingCurrencyCode("USD");
        properties.setFxValuationPolicies(List.of(
                new FxValuationPolicy.Rule("11000", date, date, FxValuationPolicy.Treatment.MONETARY),
                new FxValuationPolicy.Rule("41000", date, date, FxValuationPolicy.Treatment.HISTORICAL_COST)));
        var service = new FxValuationService((from, to, at) -> Optional.of(new BigDecimal("1.2")),
                journals, properties, new FxValuationEligibilityResolver(properties, metadata));

        // Both legs of the same foreign-currency sale must be considered together: revenue stays historical.
        service.processFxValuationForAccount(new FxValuationBalance("11000", "EUR",
                new BigDecimal("100"), new BigDecimal("110")), date, 780L);
        service.processFxValuationForAccount(new FxValuationBalance("41000", "EUR",
                new BigDecimal("-100"), new BigDecimal("-110")), date, 780L);

        var commands = ArgumentCaptor.forClass(ClosingJournalEntryCommand.class);
        verify(journals, atLeastOnce()).createDraftAdjustment(commands.capture());
        assertThat(commands.getAllValues()).singleElement().satisfies(command -> {
            assertThat(command.lines().get(0).accountCode()).isEqualTo("11000");
            assertThat(command.lines().get(0).amount()).isEqualByComparingTo("10");
            assertThat(command.lines().get(1).side()).isEqualTo(ClosingJournalSide.CREDIT);
        });
    }
}
