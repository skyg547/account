package com.ho.account.masterdata.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MasterDataChangeRequestControllerTest {

    private static final String AUTH_USER = "X-Auth-User";
    private static final String AUTH_ROLES = "X-Auth-Roles";
    private static final String APPROVAL_ROLE = "ROLE_MASTER_MANAGER";

    private MasterDataChangeRequestUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(MasterDataChangeRequestUseCase.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new MasterDataChangeRequestController(useCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void requestBusinessPartnerRegistrationMapsEveryCommandFieldAndReturnsRequestedStatus() throws Exception {
        String payloadJson = "{\"businessPartnerCode\":\"BP-NEW-001\","
                + "\"businessPartnerName\":\"새 거래처\"}";
        MasterDataChangeRequest requested = new MasterDataChangeRequest(
                MasterDataType.BUSINESS_PARTNER,
                "BP-NEW-001",
                ChangeType.CREATE,
                LocalDate.of(2026, 8, 15),
                1,
                "trusted-registrar",
                "신규 지급 거래처 등록",
                payloadJson);
        when(useCase.requestChange(any(MasterDataChangeRequestCommand.class))).thenReturn(requested);

        mockMvc.perform(post("/api/master-data/change-requests")
                        .header(AUTH_USER, "trusted-registrar")
                        .header(AUTH_ROLES, APPROVAL_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "BUSINESS_PARTNER",
                                  "targetKey": "BP-NEW-001",
                                  "changeType": "CREATE",
                                  "effectiveDate": "2026-08-15",
                                  "requestedVersion": 1,
                                  "reason": "신규 지급 거래처 등록",
                                  "payloadJson": "{\\"businessPartnerCode\\":\\"BP-NEW-001\\",\\"businessPartnerName\\":\\"새 거래처\\"}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetType").value("BUSINESS_PARTNER"))
                .andExpect(jsonPath("$.targetKey").value("BP-NEW-001"))
                .andExpect(jsonPath("$.changeType").value("CREATE"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.effectiveDate").value("2026-08-15"))
                .andExpect(jsonPath("$.requestedVersion").value(1))
                .andExpect(jsonPath("$.requestedBy").value("trusted-registrar"))
                .andExpect(jsonPath("$.reason").value("신규 지급 거래처 등록"))
                .andExpect(jsonPath("$.payloadJson").value(payloadJson));

        ArgumentCaptor<MasterDataChangeRequestCommand> commandCaptor =
                ArgumentCaptor.forClass(MasterDataChangeRequestCommand.class);
        verify(useCase).requestChange(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new MasterDataChangeRequestCommand(
                MasterDataType.BUSINESS_PARTNER,
                "BP-NEW-001",
                ChangeType.CREATE,
                LocalDate.of(2026, 8, 15),
                1,
                "trusted-registrar",
                "신규 지급 거래처 등록",
                payloadJson));
    }

    @Test
    void pendingEndpointReturnsRequestedChangesFromUseCase() throws Exception {
        MasterDataChangeRequest requested = requestedChange();
        when(useCase.findPendingRequests()).thenReturn(List.of(requested));

        mockMvc.perform(get("/api/master-data/change-requests/pending")
                        .header(AUTH_ROLES, APPROVAL_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetType").value("BUSINESS_PARTNER"))
                .andExpect(jsonPath("$[0].targetKey").value("BP-042"))
                .andExpect(jsonPath("$[0].changeType").value("UPDATE"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].effectiveDate").value("2026-08-01"))
                .andExpect(jsonPath("$[0].requestedVersion").value(2))
                .andExpect(jsonPath("$[0].requestedBy").value("requester"))
                .andExpect(jsonPath("$[0].sourceReference").value("GOV-42"));

        verify(useCase).findPendingRequests();
    }

    @Test
    void approveEndpointDelegatesDecisionAndReturnsApprovedStatus() throws Exception {
        MasterDataChangeRequest approved = requestedChange();
        approved.approve("approver");
        when(useCase.approve(42L, "approver")).thenReturn(approved);

        mockMvc.perform(post("/api/master-data/change-requests/{requestId}/approve", 42L)
                        .header(AUTH_USER, "approver")
                        .header(AUTH_ROLES, "ROLE_ACCOUNTING_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approver": "spoofed-client-value"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetKey").value("BP-042"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value("approver"));

        verify(useCase).approve(42L, "approver");
    }

    @Test
    void rejectEndpointDelegatesReasonAndReturnsRejectedStatus() throws Exception {
        MasterDataChangeRequest rejected = requestedChange();
        rejected.reject("reviewer", "등록번호 증빙 불충분");
        when(useCase.reject(42L, "reviewer", "등록번호 증빙 불충분")).thenReturn(rejected);

        mockMvc.perform(post("/api/master-data/change-requests/{requestId}/reject", 42L)
                        .header(AUTH_USER, "reviewer")
                        .header(AUTH_ROLES, "ROLE_SYSTEM_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "등록번호 증빙 불충분"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetKey").value("BP-042"))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.approvedBy").value("reviewer"))
                .andExpect(jsonPath("$.reason").value("등록번호 증빙 불충분"));

        verify(useCase).reject(42L, "reviewer", "등록번호 증빙 불충분");
    }

    @Test
    void applyEndpointRejectsAuthenticatedUserWithoutApprovalRole() throws Exception {
        mockMvc.perform(post("/api/master-data/change-requests/{requestId}/apply", 42L)
                        .header(AUTH_ROLES, "REPORT_VIEWER"))
                .andExpect(status().isForbidden());
    }

    private MasterDataChangeRequest requestedChange() {
        return new MasterDataChangeRequest(
                MasterDataType.BUSINESS_PARTNER,
                "BP-042",
                ChangeType.UPDATE,
                LocalDate.of(2026, 8, 1),
                2,
                "requester",
                "거래처 정보 정정",
                "{\"businessPartnerName\":\"한빛상사\"}",
                "GOV-42");
    }
}
