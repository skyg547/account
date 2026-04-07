package com.ho.account.closing.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.closing.domain.PeriodLock.PeriodLockType;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.dto.*;
import com.ho.account.closing.service.ClosingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClosingController.class)
class ClosingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClosingService closingService;

    @Autowired
    private ObjectMapper objectMapper;

    private FiscalPeriod testFiscalPeriod;
    private ClosingCalendar testClosingCalendar;
    private ClosingTask testClosingTask;
    private ClosingGate testClosingGate;
    private PeriodLock testPeriodLock;
    private ReopenApproval testReopenApproval;

    @BeforeEach
    void setUp() {
        testFiscalPeriod = new FiscalPeriod();
        testFiscalPeriod.setId(1L);
        testFiscalPeriod.setFiscalYear("2023");
        testFiscalPeriod.setFiscalPeriod("12");

        testClosingCalendar = new ClosingCalendar();
        testClosingCalendar.setId(10L);
        testClosingCalendar.setFiscalYear("2023");
        testClosingCalendar.setFiscalPeriod("12");
        testClosingCalendar.setStatus(ClosingCalendarStatus.OPEN);
        testClosingCalendar.setCreatedAt(LocalDateTime.now());
        testClosingCalendar.setUpdatedAt(LocalDateTime.now());
        testClosingCalendar.setAuditUser("TEST");

        testClosingTask = new ClosingTask();
        testClosingTask.setId(20L);
        testClosingTask.setClosingCalendar(testClosingCalendar);
        testClosingTask.setName("Mandatory Task");
        testClosingTask.setMandatory(true);
        testClosingTask.setStatus(ClosingTask.ClosingTaskStatus.PENDING);
        testClosingTask.setTaskOrder(1);
        testClosingTask.setCreatedAt(LocalDateTime.now());
        testClosingTask.setUpdatedAt(LocalDateTime.now());
        testClosingTask.setAuditUser("TEST");

        testClosingGate = new ClosingGate();
        testClosingGate.setId(30L);
        testClosingGate.setClosingCalendar(testClosingCalendar);
        testClosingGate.setName("All Tasks Completed Gate");
        testClosingGate.setStatus(ClosingGate.ClosingGateStatus.PENDING);
        testClosingGate.setCreatedAt(LocalDateTime.now());
        testClosingGate.setUpdatedAt(LocalDateTime.now());
        testClosingGate.setAuditUser("TEST");

        testPeriodLock = new PeriodLock();
        testPeriodLock.setId(40L);
        testPeriodLock.setFiscalPeriod(testFiscalPeriod);
        testPeriodLock.setLockType(PeriodLockType.ALL_TRANSACTIONS);
        testPeriodLock.setLockedBy("user1");
        testPeriodLock.setLockedAt(LocalDateTime.now());

        testReopenApproval = new ReopenApproval();
        testReopenApproval.setId(50L);
        testReopenApproval.setFiscalPeriod(testFiscalPeriod);
        testReopenApproval.setRequestedBy("user2");
        testReopenApproval.setReason("Correction needed");
        testReopenApproval.setStatus(ReopenApprovalStatus.PENDING);
        testReopenApproval.setRequestedAt(LocalDateTime.now());
    }

    @Test
    void testCreateClosingCalendar() throws Exception {
        ClosingCalendarRequestDto requestDto = new ClosingCalendarRequestDto();
        requestDto.setFiscalYear("2023");
        requestDto.setFiscalPeriod("12");

        when(closingService.createClosingCalendar(any(ClosingCalendar.class))).thenReturn(testClosingCalendar);

        mockMvc.perform(post("/api/closing/calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fiscalYear").value("2023"))
                .andExpect(jsonPath("$.fiscalPeriod").value("12"));
    }

    @Test
    void testUpdateClosingCalendarStatus() throws Exception {
        ClosingCalendarStatusUpdateDto requestDto = new ClosingCalendarStatusUpdateDto();
        requestDto.setStatus(ClosingCalendarStatus.CLOSED);
        requestDto.setUser("admin");

        testClosingCalendar.setStatus(ClosingCalendarStatus.CLOSED);
        when(closingService.updateClosingCalendarStatus(anyLong(), any(ClosingCalendarStatus.class), anyString()))
                .thenReturn(testClosingCalendar);

        mockMvc.perform(put("/api/closing/calendars/{id}/status", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void testLockPeriod() throws Exception {
        PeriodLockRequestDto requestDto = new PeriodLockRequestDto();
        requestDto.setFiscalPeriodId(1L);
        requestDto.setLockType(PeriodLockType.ALL_TRANSACTIONS);
        requestDto.setUser("admin");
        requestDto.setReason("Monthly close");

        when(closingService.lockPeriod(anyLong(), any(PeriodLockType.class), anyString(), anyString()))
                .thenReturn(testPeriodLock);

        mockMvc.perform(post("/api/closing/period-locks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lockType").value("ALL_TRANSACTIONS"));
    }

    @Test
    void testRequestPeriodReopen() throws Exception {
        ReopenApprovalRequestDto requestDto = new ReopenApprovalRequestDto();
        requestDto.setFiscalPeriodId(1L);
        requestDto.setRequestedBy("user2");
        requestDto.setReason("Error in JE");

        when(closingService.requestPeriodReopen(anyLong(), anyString(), anyString()))
                .thenReturn(testReopenApproval);

        mockMvc.perform(post("/api/closing/reopen-approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testDetermineClosingStatus() throws Exception {
        ClosingStatusDetermineRequestDto requestDto = new ClosingStatusDetermineRequestDto();
        requestDto.setCalendarId(10L);
        requestDto.setUser("admin");

        testClosingCalendar.setStatus(ClosingCalendarStatus.CLOSED); // 성공 판정을 시뮬레이션
        when(closingService.determineClosingStatus(anyLong(), anyString()))
                .thenReturn(testClosingCalendar);

        mockMvc.perform(post("/api/closing/calendars/determine-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}
