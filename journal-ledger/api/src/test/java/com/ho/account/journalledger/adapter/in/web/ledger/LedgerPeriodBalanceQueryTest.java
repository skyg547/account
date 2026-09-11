package com.ho.account.journalledger.adapter.in.web.ledger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LedgerPeriodBalanceQueryTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = START.plusDays(1);

    @Mock
    private LedgerBalancePersistencePort persistencePort;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 저장 포트에만 합성 일별 행을 공급한다. 실제 서비스 계산을 HTTP 응답까지 통과시킨다.
        LedgerService service = new LedgerService(persistencePort);
        mockMvc = MockMvcBuilders.standaloneSetup(new LedgerController(service), new GlSlController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/ledger/gl/balances", "/api/v1/ledger/balances/gl"})
    @DisplayName("두 GL HTTP 경로는 실제 서비스로 기초 1000·차변 150·대변 30·기말 1120을 반환한다.")
    void glHttpQueriesUseOnlyTheEarliestOpening(String path) throws Exception {
        GlBalance first = glBalance(START, "1000.00", "100.00", "20.00");
        GlBalance second = glBalance(END, "1080.00", "50.00", "10.00");
        List<GlBalance> source = List.of(second, first);
        String currencyFilter = path.equals("/api/v1/ledger/gl/balances") ? "KRW" : null;
        when(persistencePort.findGlBalances(START, END, "10100", currencyFilter)).thenReturn(source);

        String response = mockMvc.perform(get(path)
                        .param("startDate", START.toString()).param("endDate", END.toString())
                        .param("accountCode", "10100").param("currencyCode", "KRW"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JsonNode row = assertPeriodResponse(response);
        assertThat(row.get("accountCode").asText()).isEqualTo("10100");
        assertThat(row.get("currencyCode").asText()).isEqualTo("KRW");
        assertThat(first).usingRecursiveComparison().isEqualTo(glBalance(START, "1000.00", "100.00", "20.00"));
        assertThat(second).usingRecursiveComparison().isEqualTo(glBalance(END, "1080.00", "50.00", "10.00"));
        assertThat(source).containsExactly(second, first);
        verify(persistencePort).findGlBalances(START, END, "10100", currencyFilter);
        verifyNoMoreInteractions(persistencePort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/ledger/sl/balances", "/api/v1/ledger/balances/sl"})
    @DisplayName("두 SL HTTP 경로는 실제 서비스의 기간 금액과 거래처·부서 키를 보존한다.")
    void slHttpQueriesUseOnlyTheEarliestOpening(String path) throws Exception {
        SlBalance first = slBalance(START, "1000.00", "100.00", "20.00");
        SlBalance second = slBalance(END, "1080.00", "50.00", "10.00");
        List<SlBalance> source = List.of(second, first);
        boolean hasDimensionFilters = path.equals("/api/v1/ledger/sl/balances");
        String departmentFilter = hasDimensionFilters ? "D-10" : null;
        String currencyFilter = hasDimensionFilters ? "KRW" : null;
        when(persistencePort.findSlBalances(START, END, "10100", "BP-001", departmentFilter, currencyFilter))
                .thenReturn(source);

        String response = mockMvc.perform(get(path)
                        .param("startDate", START.toString()).param("endDate", END.toString())
                        .param("accountCode", "10100").param("businessPartnerCode", "BP-001")
                        .param("deptCode", "D-10").param("currencyCode", "KRW"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JsonNode row = assertPeriodResponse(response);
        assertThat(row.get("accountCode").asText()).isEqualTo("10100");
        assertThat(row.get("businessPartnerCode").asText()).isEqualTo("BP-001");
        assertThat(row.get("departmentCode").asText()).isEqualTo("D-10");
        assertThat(row.get("currencyCode").asText()).isEqualTo("KRW");
        assertThat(first).usingRecursiveComparison().isEqualTo(slBalance(START, "1000.00", "100.00", "20.00"));
        assertThat(second).usingRecursiveComparison().isEqualTo(slBalance(END, "1080.00", "50.00", "10.00"));
        assertThat(source).containsExactly(second, first);
        verify(persistencePort).findSlBalances(START, END, "10100", "BP-001", departmentFilter, currencyFilter);
        verifyNoMoreInteractions(persistencePort);
    }

    private JsonNode assertPeriodResponse(String response) throws Exception {
        JsonNode rows = objectMapper.readTree(response);
        assertThat(rows.isArray()).isTrue();
        assertThat(rows.size()).isEqualTo(1);
        JsonNode row = rows.get(0);
        assertThat(row.get("beginningBalance").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(row.get("debitAmount").decimalValue()).isEqualByComparingTo("150.00");
        assertThat(row.get("creditAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(row.get("endingBalance").decimalValue()).isEqualByComparingTo("1120.00");
        assertThat(row.get("balanceDate").asText()).isEqualTo(START.toString());
        assertThat(row.get("period").asText()).isEqualTo("2026-09");
        return row;
    }

    private GlBalance glBalance(LocalDate date, String beginning, String debit, String credit) {
        GlBalance balance = new GlBalance();
        balance.setId(date.toEpochDay());
        balance.setAccountCode("10100");
        balance.setCurrencyCode("KRW");
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

    private SlBalance slBalance(LocalDate date, String beginning, String debit, String credit) {
        SlBalance balance = new SlBalance();
        balance.setId(date.toEpochDay());
        balance.setAccountCode("10100");
        balance.setBusinessPartnerCode("BP-001");
        balance.setDepartmentCode("D-10");
        balance.setCurrencyCode("KRW");
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
}
