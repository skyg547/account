package com.ho.account.cashflow.api;

import com.ho.account.cashflow.api.controller.CashflowForecastController;
import com.ho.account.cashflow.api.controller.CashflowStatementController;
import com.ho.account.cashflow.core.application.port.in.CashflowForecastUseCase;
import com.ho.account.cashflow.core.application.port.in.CashflowStatementUseCase;
import com.ho.account.cashflow.core.application.port.out.LedgerCashBalancePort;
import com.ho.account.cashflow.core.infrastructure.memory.InMemoryLedgerCashBalanceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("local")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CashflowApplicationTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private CashflowStatementController statementController;

    @Autowired
    private CashflowForecastController forecastController;

    @Autowired
    private CashflowStatementUseCase statementUseCase;

    @Autowired
    private CashflowForecastUseCase forecastUseCase;

    @Autowired
    private LedgerCashBalancePort ledgerCashBalancePort;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void localProfileBootsWithControllersUseCasesAndMemoryAdapters() {
        assertThat(context.isActive()).isTrue();
        assertThat(statementController).isNotNull();
        assertThat(forecastController).isNotNull();
        assertThat(statementUseCase).isNotNull();
        assertThat(forecastUseCase).isNotNull();
        assertThat(ledgerCashBalancePort).isInstanceOf(InMemoryLedgerCashBalanceAdapter.class);
    }

    @Test
    void createsAndReadsBalancedStatementThroughHttp() throws Exception {
        mockMvc.perform(post("/api/v1/cashflow/statements")
                        .contentType("application/json")
                        .content("""
                                {
                                  "statementId": "stmt-http",
                                  "fiscalYear": 2026,
                                  "fiscalPeriod": 9,
                                  "method": "DIRECT",
                                  "currency": "KRW",
                                  "generatedAt": "2026-09-23T10:00:00",
                                  "lineItems": [{
                                    "lineCode": "OP-01",
                                    "category": "COLLECTION",
                                    "activity": "OPERATING",
                                    "amount": 125.50,
                                    "currency": "KRW",
                                    "description": "receipt"
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netCashflow").value(125.50))
                .andExpect(jsonPath("$.endingCash").value(125.50));

        mockMvc.perform(get("/api/v1/cashflow/statements/stmt-http"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value("stmt-http"));
    }

    @Test
    void returnsBadRequestForAmountThatWouldRequireRounding() throws Exception {
        mockMvc.perform(post("/api/v1/cashflow/statements")
                        .contentType("application/json")
                        .content("""
                                {
                                  "statementId": "stmt-invalid-scale",
                                  "fiscalYear": 2026,
                                  "fiscalPeriod": 9,
                                  "method": "DIRECT",
                                  "currency": "KRW",
                                  "generatedAt": "2026-09-23T10:00:00",
                                  "lineItems": [{
                                    "lineCode": "OP-01",
                                    "category": "COLLECTION",
                                    "activity": "OPERATING",
                                    "amount": 1.001,
                                    "currency": "KRW"
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("amount must have at most two decimal places"));
    }

    @Test
    void returnsBadRequestWhenForecastTargetPrecedesForecastDate() throws Exception {
        mockMvc.perform(post("/api/v1/cashflow/forecasts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "forecastId": "forecast-invalid-date",
                                  "forecastDate": "2026-09-23",
                                  "targetDate": "2026-09-22",
                                  "inflowEstimate": 100.00,
                                  "outflowEstimate": 50.00,
                                  "watchThreshold": 0.00,
                                  "criticalThreshold": -100.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("targetDate must not be before forecastDate"));
    }

    @Test
    void returnsNotFoundForUnknownStatement() throws Exception {
        mockMvc.perform(get("/api/v1/cashflow/statements/missing"))
                .andExpect(status().isNotFound());
    }
}
