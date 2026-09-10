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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("GL 기간 조회는 입력 순서와 무관하게 첫 날짜 기초를 한 번만 사용한다.")
    void glPeriodBalancesUseEarliestOpeningRegardlessOfInputOrder(boolean reversed) {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = startDate.plusDays(1);
        GlBalance first = glBalance("10100", "KRW", startDate, "1000.00", "100.00", "20.00");
        GlBalance second = glBalance("10100", "KRW", endDate, "1080.00", "50.00", "10.00");
        List<GlBalance> source = reversed ? List.of(second, first) : List.of(first, second);
        when(ledgerBalancePersistencePort.findGlBalances(startDate, endDate, "10100", "KRW"))
                .thenReturn(source);

        List<GlBalance> result = ledgerService.getGlBalances(startDate, endDate, "10100", "KRW");

        assertThat(result).hasSize(1).isNotSameAs(source);
        GlBalance aggregate = result.get(0);
        assertGlAmounts(aggregate, "1000.00", "150.00", "30.00", "1120.00");
        assertThat(aggregate).isNotSameAs(first).isNotSameAs(second);
        assertThat(aggregate.getAccountCode()).isEqualTo("10100");
        assertThat(aggregate.getCurrencyCode()).isEqualTo("KRW");
        assertThat(aggregate.getBalanceDate()).isEqualTo(startDate);
        assertThat(aggregate.getPeriod()).isEqualTo(YearMonth.from(startDate));
        assertThat(first).usingRecursiveComparison()
                .isEqualTo(glBalance("10100", "KRW", startDate, "1000.00", "100.00", "20.00"));
        assertThat(second).usingRecursiveComparison()
                .isEqualTo(glBalance("10100", "KRW", endDate, "1080.00", "50.00", "10.00"));
        assertThat(source).containsExactlyElementsOf(reversed ? List.of(second, first) : List.of(first, second));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("SL 기간 조회는 입력 순서와 무관하게 첫 날짜 기초를 한 번만 사용한다.")
    void slPeriodBalancesUseEarliestOpeningRegardlessOfInputOrder(boolean reversed) {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = startDate.plusDays(1);
        SlBalance first = slBalance("10100", "BP-001", "D-10", "KRW", startDate,
                "1000.00", "100.00", "20.00");
        SlBalance second = slBalance("10100", "BP-001", "D-10", "KRW", endDate,
                "1080.00", "50.00", "10.00");
        List<SlBalance> source = reversed ? List.of(second, first) : List.of(first, second);
        when(ledgerBalancePersistencePort.findSlBalances(startDate, endDate, "10100", "BP-001", "D-10", "KRW"))
                .thenReturn(source);

        List<SlBalance> result = ledgerService.getSlBalances(startDate, endDate, "10100", "BP-001", "D-10", "KRW");

        assertThat(result).hasSize(1).isNotSameAs(source);
        SlBalance aggregate = result.get(0);
        assertSlAmounts(aggregate, "1000.00", "150.00", "30.00", "1120.00");
        assertThat(aggregate).isNotSameAs(first).isNotSameAs(second);
        assertThat(aggregate.getAccountCode()).isEqualTo("10100");
        assertThat(aggregate.getBusinessPartnerCode()).isEqualTo("BP-001");
        assertThat(aggregate.getDepartmentCode()).isEqualTo("D-10");
        assertThat(aggregate.getCurrencyCode()).isEqualTo("KRW");
        assertThat(aggregate.getBalanceDate()).isEqualTo(startDate);
        assertThat(aggregate.getPeriod()).isEqualTo(YearMonth.from(startDate));
        assertThat(first).usingRecursiveComparison().isEqualTo(slBalance(
                "10100", "BP-001", "D-10", "KRW", startDate, "1000.00", "100.00", "20.00"));
        assertThat(second).usingRecursiveComparison().isEqualTo(slBalance(
                "10100", "BP-001", "D-10", "KRW", endDate, "1080.00", "50.00", "10.00"));
        assertThat(source).containsExactlyElementsOf(reversed ? List.of(second, first) : List.of(first, second));
    }

    @Test
    @DisplayName("GL은 계정·통화마다 서로 다른 최초 날짜를 찾고 기존 그룹 순서를 유지한다.")
    void glPeriodBalancesKeepAccountAndCurrencyGroupsSeparate() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = start.plusDays(6);
        when(ledgerBalancePersistencePort.findGlBalances(start, end, null, null)).thenReturn(List.of(
                glBalance("10100", "USD", start.plusDays(5), "220.00", "30.00", "4.00"),
                glBalance("20200", "KRW", start.plusDays(3), "300.00", "33.00", "3.00"),
                glBalance("10100", "KRW", start.plusDays(4), "110.00", "20.00", "3.00"),
                glBalance("10100", "USD", start.plusDays(2), "200.00", "22.00", "2.00"),
                glBalance("20200", "KRW", end, "330.00", "40.00", "5.00"),
                glBalance("10100", "KRW", start.plusDays(1), "100.00", "11.00", "1.00")));

        List<GlBalance> result = ledgerService.getGlBalances(start, end, null, null);

        assertThat(result).extracting(GlBalance::getAccountCode, GlBalance::getCurrencyCode)
                .containsExactly(tuple("10100", "USD"), tuple("20200", "KRW"), tuple("10100", "KRW"));
        assertGlAmounts(result.get(0), "200.00", "52.00", "6.00", "246.00");
        assertGlAmounts(result.get(1), "300.00", "73.00", "8.00", "365.00");
        assertGlAmounts(result.get(2), "100.00", "31.00", "4.00", "127.00");
        assertThat(result).allSatisfy(balance -> {
            assertThat(balance.getBalanceDate()).isEqualTo(start);
            assertThat(balance.getPeriod()).isEqualTo(YearMonth.from(start));
        });
        verify(ledgerBalancePersistencePort).findGlBalances(start, end, null, null);
        verifyNoMoreInteractions(ledgerBalancePersistencePort);
    }

    @Test
    @DisplayName("SL은 계정·통화·거래처·부서와 null 차원을 각각 분리한다.")
    void slPeriodBalancesKeepAllDimensionsSeparate() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = start.plusDays(6);
        when(ledgerBalancePersistencePort.findSlBalances(start, end, null, null, null, null)).thenReturn(List.of(
                slBalance("10100", "BP-001", "D-10", "KRW", end, "110.00", "20.00", "3.00"),
                slBalance("10100", "BP-002", "D-10", "KRW", start.plusDays(2), "200.00", "22.00", "2.00"),
                slBalance("10100", "BP-001", "D-20", "KRW", end, "330.00", "40.00", "5.00"),
                slBalance("10100", "BP-001", "D-10", "USD", end, "440.00", "50.00", "6.00"),
                slBalance("20200", "BP-001", "D-10", "KRW", end, "550.00", "60.00", "7.00"),
                slBalance("10100", null, null, "KRW", end, "660.00", "70.00", "8.00"),
                slBalance("10100", "BP-001", "D-10", "KRW", start, "100.00", "11.00", "1.00"),
                slBalance("10100", "BP-002", "D-10", "KRW", end, "220.00", "30.00", "4.00"),
                slBalance("10100", "BP-001", "D-20", "KRW", start.plusDays(1), "300.00", "33.00", "3.00"),
                slBalance("10100", "BP-001", "D-10", "USD", start.plusDays(3), "400.00", "44.00", "4.00"),
                slBalance("20200", "BP-001", "D-10", "KRW", start.plusDays(4), "500.00", "55.00", "5.00"),
                slBalance("10100", null, null, "KRW", start.plusDays(5), "600.00", "66.00", "6.00")));

        List<SlBalance> result = ledgerService.getSlBalances(start, end, null, null, null, null);

        assertThat(result).extracting(SlBalance::getAccountCode, SlBalance::getBusinessPartnerCode,
                        SlBalance::getDepartmentCode, SlBalance::getCurrencyCode)
                .containsExactly(tuple("10100", "BP-001", "D-10", "KRW"),
                        tuple("10100", "BP-002", "D-10", "KRW"), tuple("10100", "BP-001", "D-20", "KRW"),
                        tuple("10100", "BP-001", "D-10", "USD"), tuple("20200", "BP-001", "D-10", "KRW"),
                        tuple("10100", null, null, "KRW"));
        assertSlAmounts(result.get(0), "100.00", "31.00", "4.00", "127.00");
        assertSlAmounts(result.get(1), "200.00", "52.00", "6.00", "246.00");
        assertSlAmounts(result.get(2), "300.00", "73.00", "8.00", "365.00");
        assertSlAmounts(result.get(3), "400.00", "94.00", "10.00", "484.00");
        assertSlAmounts(result.get(4), "500.00", "115.00", "12.00", "603.00");
        assertSlAmounts(result.get(5), "600.00", "136.00", "14.00", "722.00");
        assertThat(result).allSatisfy(balance -> {
            assertThat(balance.getBalanceDate()).isEqualTo(start);
            assertThat(balance.getPeriod()).isEqualTo(YearMonth.from(start));
        });
        verify(ledgerBalancePersistencePort).findSlBalances(start, end, null, null, null, null);
        verifyNoMoreInteractions(ledgerBalancePersistencePort);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-08-30, 2026-08-31, 2026-09-02, -1000.17, 100.03, 20.01, 50.07, 10.02, 150.10, 30.03, -880.10",
            "2026-08-30, 2026-09-01, 2026-09-04, 1000.00, 100.00, 20.00, 50.00, 10.00, 150.00, 30.00, 1120.00",
            "2026-09-01, 2026-09-01, 2026-09-10, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00"
    })
    @DisplayName("GL/SL은 월경계·날짜 간격·음수·0원을 보존하고 응답 날짜는 조회 시작일로 유지한다.")
    void periodBalancesPreserveDateBoundariesAndExactAmounts(
            LocalDate start, LocalDate firstDate, LocalDate lastDate, String beginning,
            String firstDebit, String firstCredit, String lastDebit, String lastCredit,
            String totalDebit, String totalCredit, String ending) {
        GlBalance firstGl = glBalance("10100", "KRW", firstDate, beginning, firstDebit, firstCredit);
        SlBalance firstSl = slBalance("10100", null, null, "KRW", firstDate, beginning, firstDebit, firstCredit);
        String carriedForward = firstGl.getEndingBalance().toPlainString();
        when(ledgerBalancePersistencePort.findGlBalances(start, lastDate, "10100", "KRW"))
                .thenReturn(List.of(glBalance("10100", "KRW", lastDate, carriedForward, lastDebit, lastCredit), firstGl));
        when(ledgerBalancePersistencePort.findSlBalances(start, lastDate, "10100", null, null, "KRW"))
                .thenReturn(List.of(slBalance("10100", null, null, "KRW", lastDate,
                        carriedForward, lastDebit, lastCredit), firstSl));

        List<GlBalance> glResults = ledgerService.getGlBalances(start, lastDate, "10100", "KRW");
        List<SlBalance> slResults = ledgerService.getSlBalances(start, lastDate, "10100", null, null, "KRW");

        assertThat(glResults).hasSize(1);
        assertThat(slResults).hasSize(1);
        assertGlAmounts(glResults.get(0), beginning, totalDebit, totalCredit, ending);
        assertSlAmounts(slResults.get(0), beginning, totalDebit, totalCredit, ending);
        assertThat(glResults.get(0).getBalanceDate()).isEqualTo(start);
        assertThat(slResults.get(0).getBalanceDate()).isEqualTo(start);
        assertThat(glResults.get(0).getPeriod()).isEqualTo(YearMonth.from(start));
        assertThat(slResults.get(0).getPeriod()).isEqualTo(YearMonth.from(start));
    }

    @ParameterizedTest
    @CsvSource({"0, 1, 2", "0, 2, 1", "1, 0, 2", "1, 2, 0", "2, 0, 1", "2, 1, 0"})
    @DisplayName("GL/SL은 세 날짜의 모든 입력 순서에서 소수 센트와 DECIMAL(19,2) 상한을 보존한다.")
    void periodBalancesRecalculateAfterAllMovementsWithoutIntermediateOverflow(int first, int second, int third) {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = start.plusDays(2);
        List<GlBalance> glDays = List.of(
                glBalance("10100", "KRW", start, "99999999999999998.99", "1.00", "0.00"),
                glBalance("10100", "KRW", start.plusDays(1), "99999999999999999.99", "0.00", "100.00"),
                glBalance("10100", "KRW", end, "99999999999999899.99", "100.00", "0.00"));
        List<SlBalance> slDays = List.of(
                slBalance("10100", null, null, "KRW", start, "99999999999999998.99", "1.00", "0.00"),
                slBalance("10100", null, null, "KRW", start.plusDays(1), "99999999999999999.99", "0.00", "100.00"),
                slBalance("10100", null, null, "KRW", end, "99999999999999899.99", "100.00", "0.00"));
        when(ledgerBalancePersistencePort.findGlBalances(start, end, null, null))
                .thenReturn(List.of(glDays.get(first), glDays.get(second), glDays.get(third)));
        when(ledgerBalancePersistencePort.findSlBalances(start, end, null, null, null, null))
                .thenReturn(List.of(slDays.get(first), slDays.get(second), slDays.get(third)));

        List<GlBalance> glResults = ledgerService.getGlBalances(start, end, null, null);
        List<SlBalance> slResults = ledgerService.getSlBalances(start, end, null, null, null, null);

        assertThat(glResults).hasSize(1);
        assertThat(slResults).hasSize(1);
        assertGlAmounts(glResults.get(0), "99999999999999998.99", "101.00", "100.00", "99999999999999999.99");
        assertSlAmounts(slResults.get(0), "99999999999999998.99", "101.00", "100.00", "99999999999999999.99");
    }

    @Test
    @DisplayName("단일 날짜의 GL/SL은 원본 금액과 같은 별도 응답 객체를 반환한다.")
    void singleDayBalancesKeepExistingAmountsWithoutSharingSourceObjects() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        GlBalance gl = glBalance("10100", "KRW", date, "-100.17", "10.02", "20.03");
        SlBalance sl = slBalance("10100", null, null, "KRW", date, "-100.17", "10.02", "20.03");
        when(ledgerBalancePersistencePort.findGlBalances(date, date, null, null)).thenReturn(List.of(gl));
        when(ledgerBalancePersistencePort.findSlBalances(date, date, null, null, null, null)).thenReturn(List.of(sl));

        List<GlBalance> glResults = ledgerService.getGlBalances(date, date, null, null);
        List<SlBalance> slResults = ledgerService.getSlBalances(date, date, null, null, null, null);

        assertThat(glResults).hasSize(1);
        assertThat(slResults).hasSize(1);
        assertThat(glResults.get(0)).isNotSameAs(gl);
        assertThat(slResults.get(0)).isNotSameAs(sl);
        assertGlAmounts(glResults.get(0), "-100.17", "10.02", "20.03", "-110.18");
        assertSlAmounts(slResults.get(0), "-100.17", "10.02", "20.03", "-110.18");
        assertThat(gl).usingRecursiveComparison()
                .isEqualTo(glBalance("10100", "KRW", date, "-100.17", "10.02", "20.03"));
        assertThat(sl).usingRecursiveComparison()
                .isEqualTo(slBalance("10100", null, null, "KRW", date, "-100.17", "10.02", "20.03"));
    }

    @Test
    @DisplayName("빈 GL/SL 조회는 과거 잔액을 추가 조회하거나 합성하지 않고 빈 목록을 반환한다.")
    void emptyPeriodDoesNotSynthesizeBalancesOrLookUpPreviousRows() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = start.plusDays(1);
        when(ledgerBalancePersistencePort.findGlBalances(start, end, "10100", "KRW")).thenReturn(List.of());
        when(ledgerBalancePersistencePort.findSlBalances(start, end, "10100", "BP-001", "D-10", "KRW"))
                .thenReturn(List.of());

        assertThat(ledgerService.getGlBalances(start, end, "10100", "KRW")).isEmpty();
        assertThat(ledgerService.getSlBalances(start, end, "10100", "BP-001", "D-10", "KRW")).isEmpty();

        verify(ledgerBalancePersistencePort).findGlBalances(start, end, "10100", "KRW");
        verify(ledgerBalancePersistencePort).findSlBalances(start, end, "10100", "BP-001", "D-10", "KRW");
        verifyNoMoreInteractions(ledgerBalancePersistencePort);
    }

    private GlBalance glBalance(String accountCode, String currencyCode, LocalDate date,
                                String beginning, String debit, String credit) {
        GlBalance balance = new GlBalance();
        balance.setId(date.toEpochDay());
        balance.setAccountCode(accountCode);
        balance.setCurrencyCode(currencyCode);
        balance.setBalanceDate(date);
        balance.setPeriod(YearMonth.from(date));
        balance.setBeginningBalance(new BigDecimal(beginning));
        balance.setDebitAmount(new BigDecimal(debit));
        balance.setCreditAmount(new BigDecimal(credit));
        balance.recalculate();
        balance.setCreatedAt(date.atStartOfDay());
        balance.setUpdatedAt(date.atStartOfDay());
        return balance;
    }

    private SlBalance slBalance(String accountCode, String partnerCode, String departmentCode,
                                String currencyCode, LocalDate date, String beginning, String debit, String credit) {
        SlBalance balance = new SlBalance();
        balance.setId(date.toEpochDay());
        balance.setAccountCode(accountCode);
        balance.setBusinessPartnerCode(partnerCode);
        balance.setDepartmentCode(departmentCode);
        balance.setCurrencyCode(currencyCode);
        balance.setBalanceDate(date);
        balance.setPeriod(YearMonth.from(date));
        balance.setBeginningBalance(new BigDecimal(beginning));
        balance.setDebitAmount(new BigDecimal(debit));
        balance.setCreditAmount(new BigDecimal(credit));
        balance.recalculate();
        balance.setCreatedAt(date.atStartOfDay());
        balance.setUpdatedAt(date.atStartOfDay());
        return balance;
    }

    private void assertGlAmounts(GlBalance balance, String beginning, String debit, String credit, String ending) {
        assertThat(balance.getBeginningBalance()).isEqualTo(new BigDecimal(beginning));
        assertThat(balance.getDebitAmount()).isEqualTo(new BigDecimal(debit));
        assertThat(balance.getCreditAmount()).isEqualTo(new BigDecimal(credit));
        assertThat(balance.getEndingBalance()).isEqualTo(new BigDecimal(ending));
    }

    private void assertSlAmounts(SlBalance balance, String beginning, String debit, String credit, String ending) {
        assertThat(balance.getBeginningBalance()).isEqualTo(new BigDecimal(beginning));
        assertThat(balance.getDebitAmount()).isEqualTo(new BigDecimal(debit));
        assertThat(balance.getCreditAmount()).isEqualTo(new BigDecimal(credit));
        assertThat(balance.getEndingBalance()).isEqualTo(new BigDecimal(ending));
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
