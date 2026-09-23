package com.ho.account.reconciliation.api.adapter.in.web;

import com.ho.account.reconciliation.api.dto.AutoMatchResponse;
import com.ho.account.reconciliation.api.dto.InterBranchDashboardResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontend adapter for the inter-branch banking dashboard contract. */
@RestController
@RequestMapping("/api/finance/banking/inter-branch")
public class InterBranchBankingController {

    private static final InterBranchDashboardResponse EMPTY_SNAPSHOT = new InterBranchDashboardResponse(
            0,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0,
            List.of());

    private final InterBranchDashboardResponse dashboardSnapshot;

    public InterBranchBankingController() {
        this(EMPTY_SNAPSHOT);
    }

    InterBranchBankingController(InterBranchDashboardResponse dashboardSnapshot) {
        this.dashboardSnapshot = Objects.requireNonNull(dashboardSnapshot, "dashboardSnapshot must not be null");
    }

    @GetMapping("/dashboard")
    public ResponseEntity<InterBranchDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardSnapshot);
    }

    @PostMapping("/auto-match")
    public ResponseEntity<AutoMatchResponse> runAutoMatch() {
        // No core auto-match use case is exposed yet, so this endpoint must not imply a state change.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(new AutoMatchResponse(0, AutoMatchResponse.Status.NOT_EXECUTED));
    }
}
