package com.ho.account.reconciliation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.domain.ReconciliationUnit.ReconciliationFrequency;
import com.ho.account.reconciliation.domain.ReconciliationUnit.ReconciliationType;
import com.ho.account.reconciliation.dto.ReconciliationDifferenceResolutionRequestDto;
import com.ho.account.reconciliation.dto.ReconciliationRunRequestDto;
import com.ho.account.reconciliation.service.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReconciliationController.class)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReconciliationService reconciliationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ReconciliationUnit testUnit;
    private ReconciliationRun testRun;

    @BeforeEach
    void setUp() {
        testUnit = new ReconciliationUnit();
        testUnit.setId(1L);
        testUnit.setName("Bank vs Book");
        testUnit.setDescription("Bank statement vs General Ledger reconciliation");
        testUnit.setFrequency(ReconciliationFrequency.DAILY);
        testUnit.setReconciliationType(ReconciliationType.BANK_BOOK);
        String criteriaJson = "{\"bankAccount\":\"123-456\", \"currency\":\"KRW\"}";
        testUnit.setCriteriaJson(criteriaJson);
        testUnit.setActive(true);
        testUnit.setCreatedAt(LocalDateTime.now());
        testUnit.setUpdatedAt(LocalDateTime.now());
        testUnit.setAuditUser("TEST");

        testRun = new ReconciliationRun();
        testRun.setId(1L);
        testRun.setReconciliationUnit(testUnit);
        testRun.setReconciliationDate(LocalDate.now());
        testRun.setRunStartTime(LocalDateTime.now());
        testRun.setStatus(ReconciliationRun.ReconciliationRunStatus.SUCCESS);
        testRun.setRunBy("SYSTEM");
        testRun.setCreatedAt(LocalDateTime.now());
        testRun.setUpdatedAt(LocalDateTime.now());
        testRun.setAuditUser("TEST");
    }

    @Test
    void testCreateReconciliationUnit() throws Exception {
        when(reconciliationService.createReconciliationUnit(any(ReconciliationUnit.class))).thenReturn(testUnit);

        mockMvc.perform(post("/api/reconciliation/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUnit))) // 단순화를 위해 DTO가 엔티티와 동일하다고 가정
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bank vs Book"));
    }

    @Test
    void testRunReconciliation() throws Exception {
        ReconciliationRunRequestDto requestDto = new ReconciliationRunRequestDto();
        requestDto.setReconciliationUnitId(1L);
        requestDto.setReconciliationDate(LocalDate.now());

        when(reconciliationService.performReconciliation(anyLong(), any(LocalDate.class))).thenReturn(testRun);

        mockMvc.perform(post("/api/reconciliation/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.reconciliationUnitName").value("Bank vs Book"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testAssignDifference() throws Exception {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setAssignedToUser("user123");
        difference.setSlaDueDate(LocalDateTime.now().plusDays(7));
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.ASSIGNED);
        difference.setReconciliationRun(testRun); // 실행 이력과 연결

        when(reconciliationService.assignDifference(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(difference);

        ReconciliationDifferenceAssignmentRequestDto assignmentRequest = new ReconciliationDifferenceAssignmentRequestDto();
        assignmentRequest.setDifferenceId(1L);
        assignmentRequest.setAssignedToUser("user123");
        assignmentRequest.setSlaDueDate(LocalDateTime.now().plusDays(7));

        mockMvc.perform(post("/api/reconciliation/differences/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToUser").value("user123"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void testGetDifferencesByRunId() throws Exception {
        ReconciliationDifference diff1 = new ReconciliationDifference();
        diff1.setId(1L);
        diff1.setReconciliationRun(testRun);
        diff1.setDifferenceType(ReconciliationDifference.DifferenceType.AMOUNT_MISMATCH);
        diff1.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.PENDING);

        when(reconciliationService.findDifferencesByReconciliationRunId(anyLong())).thenReturn(Collections.singletonList(diff1));

        mockMvc.perform(get("/api/reconciliation/runs/1/differences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].differenceType").value("AMOUNT_MISMATCH"));
    }

    @Test
    void testResolveDifference() throws Exception {
        ReconciliationDifference difference = new ReconciliationDifference();
        difference.setId(1L);
        difference.setReconciliationRun(testRun);
        difference.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED);
        difference.setResolvedBy("resolver1");
        difference.setResolvedAt(LocalDateTime.now());

        DifferenceReasonCode reasonCode = new DifferenceReasonCode();
        reasonCode.setId(1L);
        reasonCode.setName("일반 불일치");
        difference.setReasonCode(reasonCode);

        when(reconciliationService.resolveDifference(anyLong(), anyLong(), anyLong(),
                any(ReconciliationDifference.ReconciliationDifferenceStatus.class), anyString()))
                .thenReturn(difference);

        ReconciliationDifferenceResolutionRequestDto requestDto = new ReconciliationDifferenceResolutionRequestDto();
        requestDto.setDifferenceId(1L);
        requestDto.setReasonCodeId(1L);
        requestDto.setAdjustmentJournalEntryId(100L);
        requestDto.setStatus(ReconciliationDifference.ReconciliationDifferenceStatus.RESOLVED);
        requestDto.setResolvedBy("resolver1");

        mockMvc.perform(post("/api/reconciliation/differences/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy").value("resolver1"))
                .andExpect(jsonPath("$.reasonCodeId").value(1L));
    }
}
