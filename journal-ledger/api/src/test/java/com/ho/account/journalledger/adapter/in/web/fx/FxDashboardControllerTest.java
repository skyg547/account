package com.ho.account.journalledger.adapter.in.web.fx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.journalledger.adapter.in.web.fx.FxDashboardResponse.FxPositionDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FxDashboardControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FxDashboardController()).build();

    @Test
    void returnsCompleteDeterministicFxDashboardSnapshot() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/fx/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode dashboard = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(dashboard.size()).isEqualTo(6);
        assertDecimal(dashboard, "totalNetPosition", "4250000.00");
        assertDecimal(dashboard, "totalKrwAmount", "5846155000.00");
        assertDecimal(dashboard, "dailyValuationGainLoss", "14455000.00");
        assertDecimal(dashboard, "gainLossPercent", "15.00");

        JsonNode rates = dashboard.get("rates");
        assertThat(rates).hasSize(3);
        assertRate(rates.get(0), "USD/KRW", "1385.40", "2.40", "0.17");
        assertRate(rates.get(1), "EUR/KRW", "1487.65", "-3.15", "-0.21");
        assertRate(rates.get(2), "JPY/KRW", "894.20", "-1.10", "-0.12");

        JsonNode positions = dashboard.get("positions");
        assertThat(positions).hasSize(3);
        assertPosition(positions.get(0), "USD", "미국 달러", "2500000.00", "1375.00",
                "3463500000.00", "26000000.00", "SAFE");
        assertPosition(positions.get(1), "EUR", "유로", "700000.00", "1492.00",
                "1041355000.00", "-3045000.00", "WARNING");
        assertPosition(positions.get(2), "JPY", "일본 엔", "150000000.00", "905.00",
                "1341300000.00", "-8500000.00", "EXCEEDED");
        assertThat(List.of(
                positions.get(0).get("limitStatus").textValue(),
                positions.get(1).get("limitStatus").textValue(),
                positions.get(2).get("limitStatus").textValue()
        )).containsExactlyInAnyOrder("SAFE", "WARNING", "EXCEEDED");
    }

    @Test
    void rejectsAggregateTotalsThatDoNotMatchPositions() {
        List<FxPositionDto> positions = List.of(
                new FxPositionDto("USD", "미국 달러", new BigDecimal("10.00"), new BigDecimal("1300.00"),
                        new BigDecimal("13000.00"), new BigDecimal("100.00"), "SAFE")
        );

        assertThatThrownBy(() -> new FxDashboardResponse(
                BigDecimal.TEN, new BigDecimal("12999.99"), new BigDecimal("100.00"),
                BigDecimal.ONE, List.of(), positions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalKrwAmount must equal the sum of positions.krwAmount");

        assertThatThrownBy(() -> new FxDashboardResponse(
                BigDecimal.TEN, new BigDecimal("13000.00"), new BigDecimal("99.99"),
                BigDecimal.ONE, List.of(), positions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dailyValuationGainLoss must equal the sum of positions.valuationGainLoss");
    }

    private static void assertRate(JsonNode rate, String pair, String value, String changeAmount, String changePercent) {
        assertThat(rate.size()).isEqualTo(4);
        assertThat(rate.get("pair").textValue()).isEqualTo(pair);
        assertDecimal(rate, "rate", value);
        assertDecimal(rate, "changeAmount", changeAmount);
        assertDecimal(rate, "changePercent", changePercent);
    }

    private static void assertPosition(
            JsonNode position,
            String currencyCode,
            String currencyName,
            String foreignAmount,
            String averageRate,
            String krwAmount,
            String valuationGainLoss,
            String limitStatus
    ) {
        assertThat(position.size()).isEqualTo(7);
        assertThat(position.get("currencyCode").textValue()).isEqualTo(currencyCode);
        assertThat(position.get("currencyName").textValue()).isEqualTo(currencyName);
        assertDecimal(position, "foreignAmount", foreignAmount);
        assertDecimal(position, "averageRate", averageRate);
        assertDecimal(position, "krwAmount", krwAmount);
        assertDecimal(position, "valuationGainLoss", valuationGainLoss);
        assertThat(position.get("limitStatus").textValue()).isEqualTo(limitStatus);
    }

    private static void assertDecimal(JsonNode parent, String fieldName, String expected) {
        assertThat(parent.get(fieldName).isNumber()).isTrue();
        assertThat(parent.get(fieldName).decimalValue()).isEqualByComparingTo(new BigDecimal(expected));
    }
}
