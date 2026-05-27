package com.ho.account.ecl.core.application.service.monitoring;

import com.ho.account.ecl.core.application.port.out.CrAccountBulkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * [도메인 서비스] 신용 모니터링 및 건전성 분류 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 은행의 자산이 건강한지 매일매일 검진하는 서비스입니다. 
 * 연체가 발생한 계좌는 '환자(Stage 2/3)'로 분류하여 집중 관리하도록 상태를 변경합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditMonitoringService {

    private final CrAccountBulkPort accountBulkPort;

    /**
     * 일일 신용 모니터링 배치를 실행하여 계좌의 Staging을 업데이트합니다.
     * 
     * @param baseDate 기준일자
     */
    @Transactional
    public void runDailyMonitoring(LocalDate baseDate) {
        log.info("🚀 [Domain Service] {} 기준 신용 모니터링 및 건전성 분류 시작", baseDate);

        // 💡 [비즈니스 가이드]
        // 억 단위 데이터를 관리하기 위해 인프라 레이어의 Bulk SQL 어댑터를 사용하여 계좌 상태를 일괄 전환합니다.
        // 서비스 레이어는 구체적인 SQL 기술 대신 포트 인터페이스에만 의존합니다.

        // 1. 부도계좌(STAGE 3) 판정: 연체 90일 이상
        log.info("  -> [1단계] 부도(90일+ 연체) 계좌 식별 중...");
        int defaultCount = accountBulkPort.updateStagingByDelinquentDays(90, null, "STAGE3");
        log.info("     - 총 {}건의 부도 계좌(Stage 3) 식별 완료.", defaultCount);

        // 2. SICR(STAGE 2) 판정: 연체 30일 이상
        log.info("  -> [2단계] SICR(30일+ 연체) 감지 중...");
        int sicrCount = accountBulkPort.updateStagingByDelinquentDays(30, 90, "STAGE2");
        log.info("     - 총 {}건의 SICR 계좌(Stage 2) 식별 완료.", sicrCount);

        // 3. 조기경보 고객 기반 강제 상향(STAGE 2)
        log.info("  -> [3단계] 조기경보(CRITICAL) 기반 SICR 추가 보정 중...");
        int warningCount = accountBulkPort.updateStagingByWarningLevel("CRITICAL", "STAGE2");
        log.info("     - 총 {}건의 조기경보 기반 SICR 상향 완료.", warningCount);

        log.info("✅ [Domain Service] 일일 신용 모니터링 및 건전성 분류 완료.");
    }
}
