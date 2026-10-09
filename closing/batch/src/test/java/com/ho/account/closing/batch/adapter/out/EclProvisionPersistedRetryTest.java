package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.closing.application.port.out.AllowanceBalance;
import com.ho.account.closing.application.port.out.FxExchangeRateLookupPort;
import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.EclProvisionService;
import com.ho.account.closing.domain.EclAllowanceSummary;
import com.ho.account.closing.domain.ProvisionBatch;
import com.ho.account.closing.infrastructure.external.JournalLedgerClosingJournalEntryAdapter;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EclProvisionPersistedRetryTest {
    @Test
    void reusesPersistedTwoDecimalDraftAfterRemoteSuccessAndLocalRetry() {
        LocalDate date = LocalDate.of(2026, 9, 11);
        var posting = mock(JournalPostingPort.class);
        var query = mock(JournalQueryPort.class);
        var balances = mock(AllowanceBalanceLookupPort.class);
        var source = mock(EclAllowanceResultPort.class);
        var adapter = new JournalLedgerClosingJournalEntryAdapter(posting, query);
        var properties = new ClosingAccountingProperties();
        var rule = new ClosingAccountingProperties.EclAccountMapping();
        rule.setDebitAccountCode("510100");
        rule.setCreditAccountCode("131900");
        properties.getProvisionRules().put(ProvisionBatch.ProvisionType.ECL, rule);
        var service = new EclProvisionService(balances, adapter, properties, source, mock(FxExchangeRateLookupPort.class));
        when(balances.findCreditBalance("131900", "KRW", "KRW", date))
                .thenReturn(new AllowanceBalance("KRW", "KRW", BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal target = new BigDecimal("4808.5714");
        when(source.loadSummaries(date)).thenReturn(List.of(new EclAllowanceSummary(
                date, "seed-690", "model-690", "SYNTHETIC", "KRW", "131000",
                "131900", "510100", "410900", target, new BigDecimal("100000.00"),
                target, BigDecimal.ZERO, BigDecimal.ZERO)));

        AtomicReference<JournalEntryCommand> requested = new AtomicReference<>();
        AtomicReference<PersistedDraft> persisted = new AtomicReference<>();
        when(query.findBySlipNo(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(persisted.get()).map(PersistedDraft::header)
                        .filter(header -> header.getSlipNo().equals(invocation.getArgument(0))));
        when(query.getJournalDetails(77L)).thenAnswer(invocation -> persisted.get().lines());
        when(posting.createDraftEntry(any())).thenAnswer(invocation -> {
            JournalEntryCommand command = invocation.getArgument(0);
            requested.set(command);
            // Emulate the remote NUMERIC(19,2) storage boundary, then a lost response.
            persisted.set(persistWithTwoDecimalAmounts(command));
            throw new IllegalStateException("Remote draft committed but response was lost");
        });

        assertThatThrownBy(() -> service.processEclProvision(date, 690L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("response was lost");
        assertThat(requested.get().lines()).hasSize(2).allSatisfy(line -> {
            assertThat(line.amount()).isEqualTo(new BigDecimal("4808.57"));
            assertThat(line.baseAmount()).isEqualTo(new BigDecimal("4808.57"));
        });
        assertThatCode(() -> service.processEclProvision(date, 690L)).doesNotThrowAnyException();

        verify(posting, times(1)).createDraftEntry(any());
        verify(query, times(2)).findBySlipNo(requested.get().slipNo());
        verify(query).getJournalDetails(77L);
        assertThat(persisted.get().lines()).hasSize(2).allSatisfy(line ->
                assertThat(line.getAmount()).isEqualTo(new BigDecimal("4808.57")));
    }

    private PersistedDraft persistWithTwoDecimalAmounts(JournalEntryCommand command) {
        JournalSummary header = new JournalSummary();
        header.setId(77L);
        header.setSlipNo(command.slipNo());
        header.setSlipDate(command.slipDate());
        header.setAccountingDate(command.accountingDate());
        header.setDescription(command.description());
        header.setEntryType(command.entryType());
        header.setCurrencyCode(command.currencyCode());
        header.setLineageSourceType(command.lineageSourceType());
        header.setLineageSourceId(command.lineageSourceId());
        header.setStatus("DRAFT");
        List<JournalDetailSummary> lines = command.lines().stream().map(line -> {
            JournalDetailSummary stored = new JournalDetailSummary();
            stored.setSide(JournalSide.valueOf(line.drcrType()));
            stored.setAccountCode(line.accountCode());
            stored.setAmount(line.amount().setScale(2, RoundingMode.HALF_UP));
            stored.setBaseAmount(line.baseAmount().setScale(2, RoundingMode.HALF_UP));
            stored.setDepartmentCode(line.departmentCode());
            stored.setBusinessPartnerCode(line.businessPartnerCode());
            stored.setDetailDescription(line.detailDescription());
            return stored;
        }).toList();
        return new PersistedDraft(header, lines);
    }

    private record PersistedDraft(JournalSummary header, List<JournalDetailSummary> lines) {
    }
}
