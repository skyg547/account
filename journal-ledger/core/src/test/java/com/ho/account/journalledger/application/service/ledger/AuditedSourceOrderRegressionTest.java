package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kept source compatible with the audited revision so the same test can establish RED and GREEN. */
class AuditedSourceOrderRegressionTest {
    @Test
    void directRebuildSavesEarlierDayBeforeLaterDayEvenWhenSourceIsReversed() throws Exception {
        LocalDate first = LocalDate.of(2026, 9, 1);
        LocalDate second = first.plusDays(1);
        LedgerBalancePersistencePort port = mock(LedgerBalancePersistencePort.class);
        when(port.findPostedJournalDetailsBetween(first, second)).thenReturn(List.of(
                detail(second, "20.00"), detail(first, "100.00")));

        service(port).reaggregateLedgerBalancesForPeriod(first, second);

        // The first day's save must precede the second day's previous-balance lookup.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GlBalance>> gl = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SlBalance>> sl = ArgumentCaptor.forClass(List.class);
        verify(port, times(2)).saveGlBalances(gl.capture());
        verify(port, times(2)).saveSlBalances(sl.capture());
        assertThat(gl.getAllValues()).extracting(balances -> balances.get(0).getBalanceDate())
                .containsExactly(first, second);
        assertThat(sl.getAllValues()).extracting(balances -> balances.get(0).getBalanceDate())
                .containsExactly(first, second);
    }

    private LedgerService service(LedgerBalancePersistencePort port) throws Exception {
        for (Constructor<?> constructor : LedgerService.class.getDeclaredConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length > 0 && types[0] == LedgerBalancePersistencePort.class) {
                Object[] arguments = new Object[types.length];
                arguments[0] = port;
                for (int i = 1; i < types.length; i++) {
                    arguments[i] = mock(types[i]);
                }
                return (LedgerService) constructor.newInstance(arguments);
            }
        }
        throw new IllegalStateException("LedgerService must accept the balance persistence port");
    }

    private JournalDetail detail(LocalDate date, String amount) {
        JournalEntry journal = new JournalEntry();
        journal.setAccountingDate(date);
        journal.setCurrencyCode("KRW");
        JournalDetail detail = new JournalDetail();
        detail.setJournalEntry(journal);
        detail.setSide(JournalSide.DEBIT);
        detail.setAccountCode("10100");
        detail.setBaseAmount(new BigDecimal(amount));
        detail.setBusinessPartnerCode("BP-001");
        detail.setDepartmentCode("D-10");
        return detail;
    }
}
