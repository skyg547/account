package com.ho.account.ecl.core.application.pipeline;

import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * [규제] 월간 자산 통합 서비스 (Monthly Asset Consolidation Service)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 한 달 동안 발생한 모든 자산 변동 내역을 확정하고, 분산된 데이터를 하나로 뭉쳐서(Consolidation)
 * 경영진이 리스크 현황을 한눈에 파악할 수 있는 최상위 보고 데이터를 만드는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyAssetConsolidationService {

    private final CrBulkOperationPort bulkOperationPort;

    /**
     * 월말 기준 대손충당금(IFRS9) 산출 결과를 집계하여 전략적 마트(Mart)를 생성합니다.
     */
    @Transactional
    public void consolidateMonthlyAssets(LocalDate baseDate) {
        log.info("🚀 [Consolidation] {} 기준 대손충당금(IFRS9) 산출 결과 집계 및 마트 생성 시작", baseDate);

        // 1. 기존 집계 데이터 초기화 및 집정계 적재 (Bulk Aggregation)
        // 💡 [아키텍처 변경] 원시 SQL 쿼리는 인프라 계층(CrBulkOperationAdapter)으로 격리되었습니다.
        int summaryCount = bulkOperationPort.aggregateMonthlyRiskMart(baseDate);
        
        log.info("✅ [Consolidation] 총 {}개의 리스크 요약 세그먼트가 생성되었습니다.", summaryCount);
    }
}
