package com.ho.account.ecl.api.port;

import java.util.Map;

/**
 * [Port] 배치 작업 직접 실행기 (Optional Runner).
 *
 * 💡 [교육적 주석: MSA 프로세스 & 테스트 격리 전략]
 * 테스트 환경이나 통합 로컬 러너 환경에서 Spring Batch JobLauncher 등을 직접 구동할 때 사용합니다.
 * 프로덕션 독립 API 환경에서는 이 빈이 존재하지 않아 비동기 외부 트리거/DB 큐 모드로 동작합니다.
 */
@FunctionalInterface
public interface BatchJobExecutor {

    /**
     * 지정된 이름의 배치를 실행합니다.
     *
     * @param jobName 실행할 작업 명칭
     * @param parameters 실행 파라미터
     * @return 배치 트리거 결과 응답
     */
    BatchTriggerResponse execute(String jobName, Map<String, Object> parameters);
}
