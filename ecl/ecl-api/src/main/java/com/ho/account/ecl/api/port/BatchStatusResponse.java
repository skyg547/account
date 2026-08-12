package com.ho.account.ecl.api.port;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * [DTO] 배치 모니터링 상태 응답 객체
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusResponse {
    private Long jobId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double progress;
    private List<StepStatusDto> steps;

    public static BatchStatusResponse empty() {
        return BatchStatusResponse.builder()
                .status("NO_HISTORY")
                .progress(0.0)
                .steps(Collections.emptyList())
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepStatusDto {
        private int id;
        private String name;
        private String status;
        private String exitCode;
        private long readCount;
        private long writeCount;
        private double progress;
    }
}
