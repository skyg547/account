package com.ho.account.mart.core.infrastructure.messaging;

import com.ho.account.shared.finance.event.CdmDataReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * [Event Publisher] CDM 데이터 완료 이벤트 발행기
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 컴포넌트는 '우체국' 역할을 합니다. 
 * 데이터 적재가 완료되었다는 소식을 'risk-cdm-events'라는 전용 우편함(Kafka Topic)에 넣습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdmEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "risk-cdm-events";

    public void publishDataReady(LocalDate baseDate, String traceId) {
        CdmDataReadyEvent event = new CdmDataReadyEvent(baseDate, traceId);
        log.info("🚀 [Event Publisher] CDM 데이터 완료 이벤트 발행 시작: {}", event);

        kafkaTemplate.send(TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("✅ [Event Publisher] 이벤트 발행 성공 (Offset: {})", 
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("🛑 [Event Publisher] 이벤트 발행 실패", ex);
                    }
                });
    }
}
