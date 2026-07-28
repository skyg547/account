package com.ho.account.ecl.api.infrastructure.messaging;

import com.ho.account.shared.finance.event.CdmDataReadyEvent;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * [Event Consumer] CDM 데이터 완료 알림 수신기
 *
 * <p>같은 eventId가 재전달되면 같은 Spring Batch JobInstance로 식별됩니다. 이미 완료된 이벤트는
 * 정상 중복으로 종료하고, 그 밖의 Job 실패는 Kafka listener까지 전파해 재시도/DLT 정책이
 * 동작하게 합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdmDataReadyConsumer {

    private final JobLauncher jobLauncher;
    private final Job allowanceEclJob;

    /**
     * Kafka Topic으로부터 이벤트를 수신합니다.
     *
     * <p>@todo 운영 전 baseDate/eventId 멱등 키를 사용하는 분산락 포트와 Redis/JDBC 어댑터를 연결하고,
     * owner token 기반 안전 해제와 lease 갱신을 통합 테스트합니다.</p>
     */
    @KafkaListener(topics = "allowance-cdm-events", groupId = "ifrs9-allowance-group")
    public void handleCdmDataReady(CdmDataReadyEvent event) {
        log.info(
                "CDM 완료 이벤트 수신: eventId={}, baseDate={}",
                event.getEventId(),
                event.getBaseDate());

        JobParameters parameters = new JobParametersBuilder()
                .addString(
                        "baseDate",
                        event.getBaseDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .addString("traceId", event.getTraceId())
                .addString("eventId", event.getEventId())
                .toJobParameters();

        try {
            log.info(
                    "IFRS 9 대손충당금 배치 실행: eventId={}, baseDate={}",
                    event.getEventId(),
                    event.getBaseDate());
            jobLauncher.run(allowanceEclJob, parameters);
        } catch (JobInstanceAlreadyCompleteException duplicate) {
            log.info(
                    "이미 완료된 CDM 이벤트 재전달 무시: eventId={}, baseDate={}",
                    event.getEventId(),
                    event.getBaseDate());
        } catch (Exception exception) {
            log.error(
                    "IFRS 9 대손충당금 배치 실행 실패: eventId={}, baseDate={}",
                    event.getEventId(),
                    event.getBaseDate(),
                    exception);
            throw new IllegalStateException(
                    "Allowance ECL batch failed for eventId=" + event.getEventId(),
                    exception);
        }
    }
}