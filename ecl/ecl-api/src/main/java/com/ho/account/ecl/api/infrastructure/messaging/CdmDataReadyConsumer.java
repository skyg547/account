package com.ho.account.ecl.api.infrastructure.messaging;

import com.ho.account.shared.finance.event.CdmDataReadyEvent;
import com.ho.account.shared.finance.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * [Event Consumer] CDM 데이터 완료 알림 수신기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdmDataReadyConsumer {

    private final JobLauncher jobLauncher;
    private final Job allowanceEclJob;

    /**
     * Kafka Topic으로부터 이벤트를 수신합니다.
     * 💡 [분산 락 적용] 전사적으로 동일한 기준일의 배치가 중복 실행되지 않도록 '자물쇠'를 겁니다.
     */
    @KafkaListener(topics = "allowance-cdm-events", groupId = "ifrs9-allowance-group")
    @DistributedLock(key = "allowance-batch-trigger", waitTime = 1, leaseTime = 3600) // 1시간 동안 락 유지 (충분한 배치 시간 확보)
    public void handleCdmDataReady(CdmDataReadyEvent event) {
        log.info("📩 [Event Consumer] CDM 완료 이벤트 수신 (락 획득 성공): {}", event);

        try {
            // 💡 배치를 실행할 때 사용할 파라미터를 생성합니다. (기준일, Trace ID 등)
            JobParameters params = new JobParametersBuilder()
                    .addString("baseDate", event.getBaseDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .addString("traceId", event.getTraceId())
                    .addLong("timestamp", System.currentTimeMillis()) // 중복 실행 방지 및 고유성 확보
                    .toJobParameters();

            log.info("🚀 [Event Consumer] 대손충당금(IFRS9) 산출 배치를 자동 실행합니다. (기준일: {})", event.getBaseDate());
            jobLauncher.run(allowanceEclJob, params);
            
        } catch (Exception e) {
            log.error("🛑 [Event Consumer] 배치 자동 실행 중 오류 발생", e);
        }
    }
}
