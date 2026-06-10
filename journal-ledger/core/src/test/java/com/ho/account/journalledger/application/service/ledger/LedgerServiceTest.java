package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerBalancePersistencePort ledgerBalancePersistencePort;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerBalancePersistencePort);
    }

    @Test
    @DisplayName("재집계는 일별 계정/통화/거래처/부서 합계를 만든 뒤 bulk 저장 포트로 전달한다.")
    void updateLedgerBalancesBulkSavesAggregatedBalancesThroughBulkPort() {
        LocalDate accountingDate = LocalDate.of(2026, 6, 10);
        when(ledgerBalancePersistencePort.findGlBalance(
                anyString(), anyString(), any(LocalDate.class), any(YearMonth.class)))
                .thenReturn(Optional.empty());
        when(ledgerBalancePersistencePort.findPreviousGlBalance(
                anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(ledgerBalancePersistencePort.findSlBalance(
                anyString(), anyString(), anyString(), anyString(), any(LocalDate.class), any(YearMonth.class)))
                .thenReturn(Optional.empty());
        when(ledgerBalancePersistencePort.findPreviousSlBalance(
                anyString(), anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        ledgerService.updateLedgerBalancesBulk(List.of(
                detail(accountingDate, JournalSide.DEBIT, "10100", "125.00"),
                detail(accountingDate, JournalSide.CREDIT, "10100", "25.00")
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GlBalance>> glCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SlBalance>> slCaptor = ArgumentCaptor.forClass(List.class);

        verify(ledgerBalancePersistencePort).saveGlBalances(glCaptor.capture());
        verify(ledgerBalancePersistencePort).saveSlBalances(slCaptor.capture());
        verify(ledgerBalancePersistencePort, never()).saveGlBalance(any(GlBalance.class));
        verify(ledgerBalancePersistencePort, never()).saveSlBalance(any(SlBalance.class));

        List<GlBalance> glBalances = glCaptor.getValue();
        assertThat(glBalances).hasSize(1);
        assertThat(glBalances.get(0).getAccountCode()).isEqualTo("10100");
        assertThat(glBalances.get(0).getCurrencyCode()).isEqualTo("KRW");
        assertThat(glBalances.get(0).getBalanceDate()).isEqualTo(accountingDate);
        assertThat(glBalances.get(0).getDebitAmount()).isEqualByComparingTo("125.00");
        assertThat(glBalances.get(0).getCreditAmount()).isEqualByComparingTo("25.00");
        assertThat(glBalances.get(0).getEndingBalance()).isEqualByComparingTo("100.00");

        List<SlBalance> slBalances = slCaptor.getValue();
        assertThat(slBalances).hasSize(1);
        assertThat(slBalances.get(0).getBusinessPartnerCode()).isEqualTo("BP-001");
        assertThat(slBalances.get(0).getDepartmentCode()).isEqualTo("D-10");
        assertThat(slBalances.get(0).getDebitAmount()).isEqualByComparingTo("125.00");
        assertThat(slBalances.get(0).getCreditAmount()).isEqualByComparingTo("25.00");
        assertThat(slBalances.get(0).getEndingBalance()).isEqualByComparingTo("100.00");
    }

    private JournalDetail detail(LocalDate accountingDate, JournalSide side, String accountCode, String baseAmount) {
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(accountingDate);
        entry.setCurrencyCode("KRW");

        JournalDetail detail = new JournalDetail();
        detail.setJournalEntry(entry);
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setBaseAmount(new BigDecimal(baseAmount));
        detail.setBusinessPartnerCode("BP-001");
        detail.setDepartmentCode("D-10");
        return detail;
    }
}
