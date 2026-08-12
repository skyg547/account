package com.ho.account.ecl.api.port;

import java.util.Map;

/**
 * [Port] 배치 트리거 및 상태 조회 포트.
 *
 * 💡 [교육적 주석: MSA 프로세스 & 리소스 격리 (Resource Isolation)]
 * ====================================================================================
 * 기존 Monolithic 구조에서는 API 서버 내부에서 Spring Batch의 JobLauncher 및 Job Bean을 직접 주입받아
 * 동일한 JVM 프로세스 내에서 대용량 배치(IFRS 9 대손충당금 산출 등)를 구동했습니다.
 *
 * 하지만 MSA(Microservice Architecture) 환경에서는 이러한 구조가 다음과 같은 심각한 아키텍처 결함을 유발합니다:
 *  1. 리소스 경합 (Resource Contention): 배치가 실행되는 동안 대량의 데이터 처리로 인해 CPU, 메모리, DB 커넥션 풀이
 *     고갈되어 실시간 API 요청(HTTP REST API)을 처리하는 웹 쓰레드가 지연되거나 OOM(Out Of Memory)이 발생합니다.
 *  2. 프로세스 격리 실패 (Lack of Process Isolation): 배치 오류나 메모리 누수로 인해 API 프로세스 전체가 둔화/다운됩니다.
 *  3. 독립적 스케일링 불가: API 서버는 stateless하므로 수평 확장(Scale-out)이 자유로워야 하지만,
 *     배치 실행 로직이 얽혀 있으면 독립적인 오토스케일링 및 컨테이너 관리가 불가능해집니다.
 *
 * 따라서 ecl-api 모듈은 ecl-batch 모듈을 direct project dependency로 참조하지 않으며,
 * 본 BatchTriggerPort 인터페이스를 통해 비동기 이벤트, REST API, Webhook 또는 메시지 큐 방식으로
 * 외부의 독립된 Batch 전용 프로세스(ecl-batch)에 작업 실행을 위임(Trigger)합니다.
 * ====================================================================================
 */
public interface BatchTriggerPort {

    /**
     * 지정된 이름의 배치를 비동기로 트리거 요청합니다.
     *
     * @param jobName 실행할 배치 작업 명칭 (예: "allowanceEclJob")
     * @param parameters 배치 실행에 필요한 파라미터 맵 (baseDate, traceId, eventId 등)
     * @return 배치 트리거 결과 응답 객체
     * @throws BatchAlreadyCompletedException 이미 완료된 멱등 작업인 경우
     * @throws BatchExecutionException 트리거 실패 시
     */
    BatchTriggerResponse triggerBatch(String jobName, Map<String, Object> parameters);

    /**
     * 최근 배치 실행 상태 및 각 단계별 진척 상황을 조회합니다.
     *
     * @param jobName 조회할 배치 작업 명칭
     * @return 배치 상태 응답 객체
     */
    BatchStatusResponse getBatchStatus(String jobName);
}
