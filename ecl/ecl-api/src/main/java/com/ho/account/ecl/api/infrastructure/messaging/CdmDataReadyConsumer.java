package com.ho.account.ecl.api.infrastructure.messaging;

import com.ho.account.ecl.api.port.BatchAlreadyCompletedException;
import com.ho.account.ecl.api.port.BatchTriggerPort;
import com.ho.account.shared.finance.event.CdmDataReadyEvent;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * [Event Consumer] CDM 데이터 완료 알림 수신기
 *
 * <p>💡 [교육적 주석: 메시지 기반 이벤트 비동기 트리거 & MSA 프로세스 분리]
 * 같은 eventId가 재전달되면 BatchTriggerPort에서 이미 완료된 이벤트(BatchAlreadyCompletedException)로
 * 판단하여 멱등(Idempotent)하게 무시합니다. 그 밖의 배치 트리거 실패는 Kafka Listener까지 전파되어
 * Kafka의 재시도(Retry) 및 DLT(Dead Letter Topic) 메커니즘이 원활히 구동되도록 설계되었습니다.
 * API 프로세스는 배치 JobLauncher를 직접 실행하지 않고 외부 배치 프로세스에 비동기 트리거를 위임합니다.</p>
 */
@Slf4j
@Component
public class CdmDataReadyConsumer {

    private final BatchTriggerPort batchTriggerPort;

    public CdmDataReadyConsumer(BatchTriggerPort batchTriggerPort) {
        this.batchTriggerPort = batchTriggerPort;
    }

    /**
     * Kafka Topic으로부터 이벤트를 수신합니다.
     */
    @KafkaListener(topics = "allowance-cdm-events", groupId = "ifrs9-allowance-group")
    public void handleCdmDataReady(CdmDataReadyEvent event) {
        log.info(
                "CDM 완료 이벤트 수신: eventId={}, baseDate={}",
                event.getEventId(),
                event.getBaseDate());

        Map<String, Object> parameters = Map.of(
                "baseDate", event.getBaseDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "traceId", event.getTraceId(),
                "eventId", event.getEventId()
        );

        try {
            log.info(
                    "IFRS 9 대손충당금 배치 비동기 트리거 요청: eventId={}, baseDate={}",
                    event.getEventId(),
                    event.getBaseDate());
            batchTriggerPort.triggerBatch("allowanceEclJob", parameters);
        } catch (BatchAlreadyCompletedException duplicate) {
            log.info(
                    "이미 완료된 CDM 이벤트 재전달 무시: eventId={}, baseDate={}",
                    event.getEventId(),
                    event.getBaseDate());
        } catch (Exception exception) {
            log.error(
                    "IFRS 9 대손충당금 배치 트리거 실패: eventId={}, baseDate={}",
                    event.getEventId(),
                    event.getBaseDate(),
                    exception);
            throw new IllegalStateException(
                    "Allowance ECL batch failed for eventId=" + event.getEventId(),
                    exception);
        }
    }
}
