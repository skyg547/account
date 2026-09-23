package com.ho.account.journalledger.adapter.in.web.fx;

import com.ho.account.journalledger.adapter.in.web.fx.FxDashboardResponse.FxPositionDto;
import com.ho.account.journalledger.adapter.in.web.fx.FxDashboardResponse.FxRateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/fx")
public class FxDashboardController {

    // This deterministic read-only scaffold is isolated here until a market-data use case is introduced.
    private static final FxDashboardResponse CURRENT_SNAPSHOT = new FxDashboardResponse(
            new BigDecimal("4250000.00"),
            new BigDecimal("5846155000.00"),
            new BigDecimal("14455000.00"),
            new BigDecimal("15.00"),
            List.of(
                    new FxRateDto("USD/KRW", new BigDecimal("1385.40"), new BigDecimal("2.40"), new BigDecimal("0.17")),
                    new FxRateDto("EUR/KRW", new BigDecimal("1487.65"), new BigDecimal("-3.15"), new BigDecimal("-0.21")),
                    new FxRateDto("JPY/KRW", new BigDecimal("894.20"), new BigDecimal("-1.10"), new BigDecimal("-0.12"))
            ),
            List.of(
                    new FxPositionDto("USD", "미국 달러", new BigDecimal("2500000.00"), new BigDecimal("1375.00"),
                            new BigDecimal("3463500000.00"), new BigDecimal("26000000.00"), "SAFE"),
                    new FxPositionDto("EUR", "유로", new BigDecimal("700000.00"), new BigDecimal("1492.00"),
                            new BigDecimal("1041355000.00"), new BigDecimal("-3045000.00"), "WARNING"),
                    new FxPositionDto("JPY", "일본 엔", new BigDecimal("150000000.00"), new BigDecimal("905.00"),
                            new BigDecimal("1341300000.00"), new BigDecimal("-8500000.00"), "EXCEEDED")
            )
    );

    @GetMapping("/dashboard")
    public ResponseEntity<FxDashboardResponse> getDashboard() {
        return ResponseEntity.ok(CURRENT_SNAPSHOT);
    }
}
