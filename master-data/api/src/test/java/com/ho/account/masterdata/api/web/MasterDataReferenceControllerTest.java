package com.ho.account.masterdata.api.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MasterDataReferenceControllerTest {

    private MasterDataQueryPort masterDataQueryPort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        masterDataQueryPort = mock(MasterDataQueryPort.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MasterDataReferenceController(masterDataQueryPort))
                .build();
    }

    @Test
    void forwardsHistoricalEffectiveDateWithoutReplacingItWithToday() throws Exception {
        LocalDate historicalDate = LocalDate.of(2024, 12, 31);
        when(masterDataQueryPort.findAccountSubjectAt("1100", historicalDate))
                .thenReturn(Optional.of(new AccountSubjectRef(
                        "1100", "Historical cash", false, false, "DEBIT", "ASSETS")));

        mockMvc.perform(get("/api/basic/references/account-subjects/1100")
                        .queryParam("effectiveDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Historical cash"));

        verify(masterDataQueryPort).findAccountSubjectAt("1100", historicalDate);
    }

    @Test
    void returnsNotFoundForReferenceMissingAtRequestedDate() throws Exception {
        LocalDate historicalDate = LocalDate.of(2020, 1, 1);
        when(masterDataQueryPort.findAccountSubjectAt("1100", historicalDate))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/basic/references/account-subjects/1100")
                        .queryParam("effectiveDate", "2020-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void forwardsBusinessPartnerHistoricalEffectiveDate() throws Exception {
        LocalDate historicalDate = LocalDate.of(2024, 12, 31);
        when(masterDataQueryPort.findBusinessPartnerAt("BP-1", historicalDate))
                .thenReturn(Optional.of(new BusinessPartnerRef(
                        "BP-1", "Historical partner", "CORPORATION", true)));

        mockMvc.perform(get("/api/basic/references/business-partners/BP-1")
                        .queryParam("effectiveDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BP-1"))
                .andExpect(jsonPath("$.active").value(true));

        verify(masterDataQueryPort).findBusinessPartnerAt("BP-1", historicalDate);
    }

    @Test
    void forwardsDepartmentHistoricalEffectiveDate() throws Exception {
        LocalDate historicalDate = LocalDate.of(2024, 12, 31);
        when(masterDataQueryPort.findDepartmentAt("D-1", historicalDate))
                .thenReturn(Optional.of(new DepartmentRef(
                        "D-1", "Historical department", "COST_CENTER")));

        mockMvc.perform(get("/api/basic/references/departments/D-1")
                        .queryParam("effectiveDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("D-1"));

        verify(masterDataQueryPort).findDepartmentAt("D-1", historicalDate);
    }

    @Test
    void rejectsMissingEffectiveDate() throws Exception {
        mockMvc.perform(get("/api/basic/references/account-subjects/1100"))
                .andExpect(status().isBadRequest());
    }
}
