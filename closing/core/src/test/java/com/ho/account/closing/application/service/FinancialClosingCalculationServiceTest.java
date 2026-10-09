package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.FinancialClosingCalculationResult;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.FxValuationEvidencePort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialClosingCalculationServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 5, 31);

    @Mock
    private FxValuationEvidencePort evidence;
    @Mock
    private FxValuationService fx;
    @Mock
    private EclProvisionService ecl;

    private ClosingAccountingProperties properties;
    private FinancialClosingCalculationService service;

    @BeforeEach
    void setUp() {
        properties = new ClosingAccountingProperties();
        service = new FinancialClosingCalculationService(evidence, fx, ecl, properties);
    }

    @Test
    void laterMissingFxRateFailsCompletePreflightBeforeAnyJournalWrite() {
        FxValuationBalance first = balance("11000", "USD");
        FxValuationBalance later = balance("12000", "EUR");
        emit(first, later);
        when(fx.prepareFxValuationForAccount(first, DATE, 77L)).thenReturn(Optional.of(command("FX-1")));
        when(fx.prepareFxValuationForAccount(later, DATE, 77L))
                .thenThrow(new IllegalStateException("FX rate not found for EUR"));

        assertThatThrownBy(() -> service.runFxValuation(DATE, 77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FX rate not found");

        verify(fx, never()).postPreparedFxValuation(any());
    }

    @Test
    void evidenceCapFailsBeforeAnyJournalWrite() {
        properties.setApiFinancialRunMaxEvidenceRows(1);
        FxValuationBalance first = balance("11000", "USD");
        FxValuationBalance second = balance("12000", "EUR");
        emit(first, second);
        when(fx.prepareFxValuationForAccount(first, DATE, 77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runFxValuation(DATE, 77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source evidence exceeds API hard cap of 1");

        verify(fx, never()).postPreparedFxValuation(any());
    }

    @Test
    void commandCapFailsAfterPreparationAndBeforeAnyJournalWrite() {
        properties.setApiFinancialRunMaxJournalCommands(1);
        FxValuationBalance first = balance("11000", "USD");
        FxValuationBalance second = balance("12000", "EUR");
        emit(first, second);
        when(fx.prepareFxValuationForAccount(first, DATE, 77L)).thenReturn(Optional.of(command("FX-1")));
        when(fx.prepareFxValuationForAccount(second, DATE, 77L)).thenReturn(Optional.of(command("FX-2")));

        assertThatThrownBy(() -> service.runFxValuation(DATE, 77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("journal commands exceed API hard cap of 1");

        verify(fx, never()).postPreparedFxValuation(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void postsExactPreparedCommandsAndReportsZeroOneOrManyResults(int commandCount) {
        int evidenceCount = Math.max(1, commandCount);
        List<FxValuationBalance> balances = new ArrayList<>();
        List<ClosingJournalEntryCommand> commands = new ArrayList<>();
        for (int index = 0; index < evidenceCount; index++) {
            FxValuationBalance balance = balance("11" + index + "00", index % 2 == 0 ? "USD" : "EUR");
            balances.add(balance);
            if (index < commandCount) {
                ClosingJournalEntryCommand command = command("FX-" + index);
                commands.add(command);
                when(fx.prepareFxValuationForAccount(balance, DATE, 77L)).thenReturn(Optional.of(command));
                when(fx.postPreparedFxValuation(command))
                        .thenReturn(new ClosingJournalEntryResult(900L + index, command.slipNo()));
            } else {
                when(fx.prepareFxValuationForAccount(balance, DATE, 77L)).thenReturn(Optional.empty());
            }
        }
        emit(balances.toArray(FxValuationBalance[]::new));

        FinancialClosingCalculationResult result = service.runFxValuation(DATE, 77L);

        assertThat(result.journalCount()).isEqualTo(commandCount);
        assertThat(result.singleJournalEntryId()).isEqualTo(commandCount == 1 ? 900L : null);
        if (commands.isEmpty()) {
            verify(fx, never()).postPreparedFxValuation(any());
        } else {
            var order = inOrder(fx);
            commands.forEach(command -> order.verify(fx).postPreparedFxValuation(command));
        }
    }

    @Test
    void eclPreparationFailureNeverInvokesPostingPhase() {
        when(ecl.prepareEclProvision(DATE, 88L))
                .thenThrow(new IllegalStateException("No finalized ECL allowance summary"));

        assertThatThrownBy(() -> service.runEclProvision(DATE, 88L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No finalized ECL allowance summary");

        verify(ecl, never()).postPreparedEclProvision(any());
        verifyNoInteractions(fx);
    }

    @Test
    void batchFxValidationDoesNotApplySynchronousApiCapsOrPost() {
        properties.setApiFinancialRunMaxEvidenceRows(1);
        properties.setApiFinancialRunMaxJournalCommands(1);
        FxValuationBalance first = balance("11000", "USD");
        FxValuationBalance second = balance("12000", "EUR");
        emit(first, second);
        when(fx.prepareFxValuationForAccount(first, DATE, 77L)).thenReturn(Optional.of(command("FX-1")));
        when(fx.prepareFxValuationForAccount(second, DATE, 77L)).thenReturn(Optional.of(command("FX-2")));

        service.validateFxValuation(DATE, 77L);

        verify(fx).prepareFxValuationForAccount(first, DATE, 77L);
        verify(fx).prepareFxValuationForAccount(second, DATE, 77L);
        verify(fx, never()).postPreparedFxValuation(any());
    }

    private void emit(FxValuationBalance... balances) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<FxValuationBalance> consumer = invocation.getArgument(2);
            for (FxValuationBalance balance : balances) {
                consumer.accept(balance);
            }
            return null;
        }).when(evidence).forEachBalance(eq(DATE), eq("KRW"), any());
    }

    private FxValuationBalance balance(String accountCode, String currencyCode) {
        return new FxValuationBalance(
                accountCode, currencyCode, new BigDecimal("100.00"), new BigDecimal("100000.00"));
    }

    private ClosingJournalEntryCommand command(String lineage) {
        BigDecimal amount = new BigDecimal("10.00");
        return new ClosingJournalEntryCommand(
                DATE, DATE, "FX test", "CLOSING_ADJUSTMENT", "SYSTEM", "SYSTEM",
                "FX_VALUATION", lineage, "KRW", "FX" + lineage.replace("-", ""),
                List.of(
                        new ClosingJournalLineCommand(
                                ClosingJournalSide.DEBIT, "11000", amount, amount, "debit"),
                        new ClosingJournalLineCommand(
                                ClosingJournalSide.CREDIT, "72000", amount, amount, "credit")));
    }
}
