package com.ho.account.ecl.api.port;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [DTO] 배치 트리거 응답 객체
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BatchTriggerResponse {
    private String jobName;
    private String executionId;
    private String status;
    private String message;

    public static BatchTriggerResponse success(String jobName, String executionId) {
        return new BatchTriggerResponse(
                jobName,
                executionId,
                "STARTED",
                jobName + " 배치가 성공적으로 시작되었습니다.");
    }
}
