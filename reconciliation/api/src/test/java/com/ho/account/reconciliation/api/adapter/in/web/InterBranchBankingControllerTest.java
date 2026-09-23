package com.ho.account.reconciliation.api.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.reconciliation.api.dto.InterBranchDashboardResponse;
import com.ho.account.reconciliation.api.dto.InterBranchTransactionResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InterBranchBankingControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InterBranchTransactionResponse transaction = new InterBranchTransactionResponse(
                101L,
                "SEOUL",
                "BUSAN",
                "INTER_BRANCH_TRANSFER",
                new BigDecimal("1250000.50"),
                InterBranchTransactionResponse.Status.DISCREPANCY);
        InterBranchDashboardResponse snapshot = new InterBranchDashboardResponse(
                1,
                new BigDecimal("1250000.50"),
                new BigDecimal("75.25"),
                2,
                List.of(transaction));
        mockMvc = MockMvcBuilders.standaloneSetup(new InterBranchBankingController(snapshot)).build();
    }

    @Test
    void getDashboardReturnsCompleteSnapshotIncludingTransactionShape() throws Exception {
        mockMvc.perform(get("/api/finance/banking/inter-branch/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unmatchedCount").value(1))
                .andExpect(jsonPath("$.totalDiscrepancyAmount").value(1250000.50))
                .andExpect(jsonPath("$.autoMatchRate").value(75.25))
                .andExpect(jsonPath("$.unexplainedDepositsCount").value(2))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].id").value(101))
                .andExpect(jsonPath("$.transactions[0].sourceBranch").value("SEOUL"))
                .andExpect(jsonPath("$.transactions[0].targetBranch").value("BUSAN"))
                .andExpect(jsonPath("$.transactions[0].transactionType").value("INTER_BRANCH_TRANSFER"))
                .andExpect(jsonPath("$.transactions[0].amount").value(1250000.50))
                .andExpect(jsonPath("$.transactions[0].status").value("DISCREPANCY"));
    }

    @Test
    void defaultControllerReturnsDeterministicEmptySnapshot() throws Exception {
        MockMvc defaultController = MockMvcBuilders
                .standaloneSetup(new InterBranchBankingController())
                .build();

        defaultController.perform(get("/api/finance/banking/inter-branch/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unmatchedCount").value(0))
                .andExpect(jsonPath("$.totalDiscrepancyAmount").value(0))
                .andExpect(jsonPath("$.autoMatchRate").value(0))
                .andExpect(jsonPath("$.unexplainedDepositsCount").value(0))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions").isEmpty());
    }

    @Test
    void runAutoMatchReturnsExplicitNonMutatingResult() throws Exception {
        mockMvc.perform(post("/api/finance/banking/inter-branch/auto-match"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.status").value("NOT_EXECUTED"));
    }
}
