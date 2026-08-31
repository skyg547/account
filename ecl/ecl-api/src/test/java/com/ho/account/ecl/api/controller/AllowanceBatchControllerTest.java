package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.api.port.BatchStatusResponse;
import com.ho.account.ecl.api.port.BatchTriggerPort;
import com.ho.account.ecl.api.port.BatchTriggerResponse;
import com.ho.account.ecl.core.application.service.calculation.EadBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AllowanceBatchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BatchTriggerPort batchTriggerPort;

    @Mock
    private EadBatchService eadBatchService;

    @InjectMocks
    private AllowanceBatchController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /status 엔드포인트는 BatchTriggerPort로부터 조회된 파이프라인 상태를 반환한다")
    void getBatchStatus_returnsPipelineStatus() throws Exception {
        BatchStatusResponse response = BatchStatusResponse.builder()
                .jobId(123L)
                .status("COMPLETED")
                .startTime(LocalDateTime.of(2026, 4, 15, 10, 0))
                .endTime(LocalDateTime.of(2026, 4, 15, 10, 5))
                .progress(100.0)
                .steps(List.of(
                        BatchStatusResponse.StepStatusDto.builder()
                                .id(1)
                                .name("allowanceExposureSyncStep")
                                .status("COMPLETED")
                                .readCount(10)
                                .writeCount(10)
                                .progress(100.0)
                                .build()
                ))
                .build();

        given(batchTriggerPort.getBatchStatus("allowanceEclJob")).willReturn(response);

        mockMvc.perform(get("/api/v1/ifrs/allowance/batch/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(123))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100.0))
                .andExpect(jsonPath("$.steps[0].name").value("allowanceExposureSyncStep"))
                .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /run 엔드포인트는 BatchTriggerPort에 비동기 배치를 트리거 요청한다")
    void runBatch_triggersBatchExecution() throws Exception {
        given(batchTriggerPort.triggerBatch(eq("allowanceEclJob"), any()))
                .willReturn(BatchTriggerResponse.success("allowanceEclJob", "EXT-1234"));

        mockMvc.perform(post("/api/v1/ifrs/allowance/batch/run")
                        .param("jobName", "allowanceEclJob")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("성공적으로 시작되었습니다")));
    }
}
