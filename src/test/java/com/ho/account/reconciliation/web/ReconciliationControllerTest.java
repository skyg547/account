package com.ho.account.reconciliation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.domain.ReconciliationUnit.ReconciliationFrequency;
import com.ho.account.reconciliation.domain.ReconciliationUnit.ReconciliationType;
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
        testUnit.setCriteriaJson("{"bankAccount":"123-456", "currency":"KRW"}");
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
                        .content(objectMapper.writeValueAsString(testUnit))) // Assuming DTO matches entity for simplicity
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
        difference.setReconciliationRun(testRun); // Link to a run

        when(reconciliationService.assignDifference(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(difference);

        mockMvc.perform(post("/api/reconciliation/differences/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"differenceId": 1, "assignedToUser": "user123", "slaDueDate": "" + LocalDateTime.now().plusDays(7) + ""}"))
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
}
