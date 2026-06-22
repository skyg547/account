package com.ho.account.reporting.infrastructure.filing;

import com.ho.account.reporting.domain.model.FinancialStatement;
import com.ho.account.reporting.domain.model.RegulatoryFilingLine;
import com.ho.account.reporting.domain.model.RegulatoryFilingPackage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalRegulatoryFilingGatewayAdapterTest {

    @Test
    void submitReturnsReceiptWhenFailureSimulationDisabled() {
        LocalRegulatoryFilingGatewayAdapter adapter =
                new LocalRegulatoryFilingGatewayAdapter(false, "forced rejection");

        var receipt = adapter.submit(packageFixture());

        assertThat(receipt.receiptId()).startsWith("EXT-FSS-BALANCE_SHEET-20260331-V1-");
        assertThat(receipt.message()).contains("Accepted");
    }

    @Test
    void submitFailsDeterministicallyWhenFailureSimulationEnabled() {
        LocalRegulatoryFilingGatewayAdapter adapter =
                new LocalRegulatoryFilingGatewayAdapter(true, "forced rejection");

        assertThatThrownBy(() -> adapter.submit(packageFixture()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Regulatory filing submission failed: forced rejection");
    }

    private RegulatoryFilingPackage packageFixture() {
        return new RegulatoryFilingPackage(
                "FILING-001",
                "SUB-001",
                FinancialStatement.StatementType.BALANCE_SHEET,
                LocalDateTime.of(2026, 3, 31, 0, 0),
                1,
                "FSS",
                List.of(new RegulatoryFilingLine(
                        "BS-FSS",
                        "F001",
                        "Cash",
                        "N1",
                        "CASH",
                        "Cash and deposits",
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        1)));
    }
}
