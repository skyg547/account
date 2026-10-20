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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.MasterDataChangeRequestCommand;
import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.application.query.PageSlice;
import com.ho.account.masterdata.core.application.query.PageWindow;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MasterDataChangeRequestControllerTest {

    private static final String AUTH_USER = "X-Auth-User";
    private static final String AUTH_ROLES = "X-Auth-Roles";
    private static final String APPROVAL_ROLE = "ROLE_MASTER_MANAGER";
    private static final String RAW_REGISTRATION = "SYN-REG-12345";
    private static final Set<String> SUMMARY_FIELDS = Set.of(
            "id", "targetType", "changeType", "status", "effectiveDate",
            "requestedVersion", "requestedAt", "approvedAt", "appliedAt",
            "failureReason", "failedAt");

    private MasterDataChangeRequestUseCase useCase;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        useCase = mock(MasterDataChangeRequestUseCase.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new MasterDataChangeRequestController(useCase))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void requestBusinessPartnerRegistrationMapsEveryCommandFieldAndReturnsRequestedStatus() throws Exception {
        String payloadJson = "{\"businessPartnerCode\":\"BP-NEW-001\","
                + "\"businessPartnerName\":\"새 거래처\","
                + "\"registrationNumber\":\"" + RAW_REGISTRATION + "\"}";
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

        MvcResult result = mockMvc.perform(post("/api/master-data/change-requests")
                        .header(AUTH_USER, "trusted-registrar")
                        .header(AUTH_ROLES, APPROVAL_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.LinkedHashMap<>(java.util.Map.of(
                                "targetType", "BUSINESS_PARTNER",
                                "targetKey", "BP-NEW-001",
                                "changeType", "CREATE",
                                "effectiveDate", "2026-08-15",
                                "requestedVersion", 1,
                                "reason", "신규 지급 거래처 등록",
                                "payloadJson", payloadJson)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetType").value("BUSINESS_PARTNER"))
                .andExpect(jsonPath("$.changeType").value("CREATE"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.effectiveDate").value("2026-08-15"))
                .andExpect(jsonPath("$.requestedVersion").value(1))
                .andReturn();
        assertSafeSummary(result, false);

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
        when(useCase.findPendingRequests(new PageWindow(0, 20)))
                .thenReturn(new PageSlice<>(List.of(requested), false));

        MvcResult result = mockMvc.perform(get("/api/master-data/change-requests/pending")
                        .header(AUTH_ROLES, APPROVAL_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetType").value("BUSINESS_PARTNER"))
                .andExpect(jsonPath("$[0].changeType").value("UPDATE"))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[0].effectiveDate").value("2026-08-01"))
                .andExpect(jsonPath("$[0].requestedVersion").value(2))
                .andReturn();
        assertSafeSummary(result, true);

        verify(useCase).findPendingRequests(new PageWindow(0, 20));
    }

    @Test
    void approveEndpointDelegatesDecisionAndReturnsApprovedStatus() throws Exception {
        MasterDataChangeRequest approved = requestedChange();
        approved.approve("approver");
        when(useCase.approve(42L, "approver")).thenReturn(approved);

        MvcResult result = mockMvc.perform(post("/api/master-data/change-requests/{requestId}/approve", 42L)
                        .header(AUTH_USER, "approver")
                        .header(AUTH_ROLES, "ROLE_ACCOUNTING_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approver": "spoofed-client-value"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();
        assertSafeSummary(result, false);

        verify(useCase).approve(42L, "approver");
    }

    @Test
    void rejectEndpointDelegatesReasonAndReturnsRejectedStatus() throws Exception {
        MasterDataChangeRequest rejected = requestedChange();
        rejected.reject("reviewer", "등록번호 증빙 불충분");
        when(useCase.reject(42L, "reviewer", "등록번호 증빙 불충분")).thenReturn(rejected);

        MvcResult result = mockMvc.perform(post("/api/master-data/change-requests/{requestId}/reject", 42L)
                        .header(AUTH_USER, "reviewer")
                        .header(AUTH_ROLES, "ROLE_SYSTEM_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "등록번호 증빙 불충분"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andReturn();
        assertSafeSummary(result, false);

        verify(useCase).reject(42L, "reviewer", "등록번호 증빙 불충분");
    }

    @Test
    void applyEndpointRejectsAuthenticatedUserWithoutApprovalRole() throws Exception {
        mockMvc.perform(post("/api/master-data/change-requests/{requestId}/apply", 42L)
                        .header(AUTH_ROLES, "REPORT_VIEWER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyDueEndpointReportsEachSuccessAndSafeDurableFailureCategory() throws Exception {
        MasterDataChangeRequest applied = requestedChange();
        applied.approve("approver");
        applied.markApplied();
        MasterDataChangeRequest failed = new MasterDataChangeRequest(
                MasterDataType.BUSINESS_PARTNER, "BP-INVALID", ChangeType.CREATE,
                LocalDate.of(2026, 8, 1), 1, "requester", "business reason",
                "{\"businessPartnerName\":\"synthetic\"}");
        failed.approve("approver");
        failed.markFailed(ChangeStatus.FAILED_INVALID, "INVALID_REQUEST");
        when(useCase.applyDueApprovedChanges()).thenReturn(List.of(applied, failed));

        MvcResult result = mockMvc.perform(post("/api/master-data/change-requests/apply-due")
                        .header(AUTH_ROLES, APPROVAL_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPLIED"))
                .andExpect(jsonPath("$[0].failureReason").isEmpty())
                .andExpect(jsonPath("$[1].status").value("FAILED_INVALID"))
                .andExpect(jsonPath("$[1].failureReason").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$[1].failedAt").isNotEmpty())
                .andExpect(jsonPath("$[1].appliedAt").isEmpty())
                .andReturn();
        assertSafeSummary(result, true);

        verify(useCase).applyDueApprovedChanges();
    }

    @Test
    void applyEndpointReturnsOnlySafeSummary() throws Exception {
        MasterDataChangeRequest applied = requestedChange();
        applied.approve("approver");
        applied.markApplied();
        when(useCase.applyApprovedChange(42L)).thenReturn(applied);

        MvcResult result = mockMvc.perform(post("/api/master-data/change-requests/{requestId}/apply", 42L)
                        .header(AUTH_ROLES, APPROVAL_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andReturn();
        assertSafeSummary(result, false);
        verify(useCase).applyApprovedChange(42L);
    }

    @Test
    void applyDueEndpointReturnsOnlySafeSummaries() throws Exception {
        MasterDataChangeRequest applied = requestedChange();
        applied.approve("approver");
        applied.markApplied();
        when(useCase.applyDueApprovedChanges()).thenReturn(List.of(applied));

        MvcResult result = mockMvc.perform(post("/api/master-data/change-requests/apply-due")
                        .header(AUTH_ROLES, APPROVAL_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPLIED"))
                .andReturn();
        assertSafeSummary(result, true);
        verify(useCase).applyDueApprovedChanges();
    }

    private void assertSafeSummary(MvcResult result, boolean list) throws Exception {
        String body = result.getResponse().getContentAsString();
        // Check the full wire body too: a nested escaped payload string can bypass field-level masking.
        assertThat(body).doesNotContain(RAW_REGISTRATION);
        JsonNode root = objectMapper.readTree(body);
        JsonNode summary = list ? root.get(0) : root;
        assertThat(summary).isNotNull();
        assertThat(StreamSupport.stream(Spliterators.spliteratorUnknownSize(summary.fieldNames(), 0), false)
                .toList())
                .containsExactlyInAnyOrderElementsOf(SUMMARY_FIELDS);
    }
    private MasterDataChangeRequest requestedChange() {
        return new MasterDataChangeRequest(
                MasterDataType.BUSINESS_PARTNER,
                "BP-042-" + RAW_REGISTRATION,
                ChangeType.UPDATE,
                LocalDate.of(2026, 8, 1),
                2,
                "requester-" + RAW_REGISTRATION,
                "거래처 정보 정정 " + RAW_REGISTRATION,
                "{\"registrationNumber\":\"" + RAW_REGISTRATION + "\"}",
                "GOV-42-" + RAW_REGISTRATION);
    }
}
