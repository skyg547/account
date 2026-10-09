package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.closing.application.port.out.ClosingFinancialRunManifestPort;
import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalEntryResult;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.closing.application.port.out.FxValuationEvidencePort;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.EclProvisionService;
import com.ho.account.closing.application.service.FinancialClosingCalculationService;
import com.ho.account.closing.application.service.FxValuationService;
import com.ho.account.closing.infrastructure.persistence.JpaClosingFinancialRunManifestAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(JpaClosingFinancialRunManifestAdapter.class)
class ClosingFinancialRunManifestIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 5, 31);
    @Autowired ClosingFinancialRunManifestPort manifest;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void partialDraftThenSourceOmissionReplaysPersistedCommandsWithoutDuplicate() {
        EclProvisionService ecl = mock(EclProvisionService.class);
        ClosingAccountingProperties properties = new ClosingAccountingProperties();
        FinancialClosingCalculationService calculation = new FinancialClosingCalculationService(
                mock(FxValuationEvidencePort.class), mock(FxValuationService.class), ecl, properties, manifest);
        ClosingJournalEntryCommand a = command("A");
        ClosingJournalEntryCommand b = command("B");
        when(ecl.prepareEclProvision(DATE, 88L)).thenReturn(List.of(a, b), List.of());
        Map<String, Long> draftIds = new HashMap<>();
        AtomicInteger attempts = new AtomicInteger();
        when(ecl.postPreparedEclProvision(any())).thenAnswer(invocation -> {
            List<ClosingJournalEntryCommand> commands = invocation.getArgument(0);
            assertThat(commands).containsExactly(a, b);
            int attempt = attempts.incrementAndGet();
            java.util.ArrayList<ClosingJournalEntryResult> results = new java.util.ArrayList<>();
            for (ClosingJournalEntryCommand command : commands) {
                if (attempt == 1 && command.slipNo().equals("SLIP-B")) {
                    throw new IllegalStateException("Journal failed after A draft");
                }
                Long id = draftIds.computeIfAbsent(command.slipNo(), ignored -> (long) draftIds.size() + 901L);
                results.add(new ClosingJournalEntryResult(id, command.slipNo()));
            }
            return results;
        });

        assertThatThrownBy(() -> calculation.runEclProvision(DATE, 88L))
                .hasMessageContaining("after A draft");
        assertThat(draftIds).containsEntry("SLIP-A", 901L).doesNotContainKey("SLIP-B");

        var result = calculation.runEclProvision(DATE, 88L);
        assertThat(result.journalCount()).isEqualTo(2);
        assertThat(result.singleJournalEntryId()).isNull();
        assertThat(draftIds).containsEntry("SLIP-A", 901L).containsEntry("SLIP-B", 902L);
        verify(ecl).prepareEclProvision(DATE, 88L);
        assertThat(manifest.loadOrCreate("PROVISION", 88L, List::of)).containsExactly(a, b);
    }

    private ClosingJournalEntryCommand command(String suffix) {
        BigDecimal amount = new BigDecimal("10.00");
        return new ClosingJournalEntryCommand(DATE, DATE, "ECL test", "CLOSING_ADJUSTMENT",
                "SYSTEM", "SYSTEM", "ECL_PROVISION", "88|" + suffix, "KRW", "SLIP-" + suffix,
                List.of(new ClosingJournalLineCommand(ClosingJournalSide.DEBIT, "93000", amount, amount, "debit"),
                        new ClosingJournalLineCommand(ClosingJournalSide.CREDIT, "12900", amount, amount, "credit")));
    }
}
