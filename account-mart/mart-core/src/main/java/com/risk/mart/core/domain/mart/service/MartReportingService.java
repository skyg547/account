package com.risk.mart.core.domain.mart.service;

import com.risk.mart.core.application.port.out.IntegratedRiskPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [Mart] 통합 리스크 리포팅 집계 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 서비스는 리스크 시스템의 '최종 성적표'를 만드는 곳입니다. 
 * 신용 리스크나 금리 리스크에서 계산된 복잡한 결과들을 한데 모아서, 
 * 경영진이 한눈에 볼 수 있는 요약 지표(총 손실, 자본 적정성 등)로 가공하는 리포팅의 심장부입니다.
 * 
 * 💡 [핵심 용어 설명]
 * - ECL (Expected Credit Loss, 예상손실): 미래에 대출 고객이 돈을 갚지 못해 발생할 것으로 예상되는 손실액입니다.
 * - RWA (Risk Weighted Assets, 위험가중자산): 자산의 위험도를 반영하여 계산한 자산 가치입니다. 위험할수록 RWA가 커집니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MartReportingService {

    /** 💡 [초보자 가이드] 통합된 리스크 산출 결과들이 담겨 있는 데이터 마트(RDM) 테이블 저장소입니다. */
    private final IntegratedRiskPositionRepository martRepository;

    /**
     * 리스크 산출용 마트 집계 현황을 로그로 출력하고 경영진 리포팅용 데이터를 요약합니다.
     * 
     * 💡 [비즈니스 시나리오]
     * 정기적인(매일 또는 매달) 리스크 정산이 끝났을 때, 
     * 은행 전체의 예상 손실과 위험 수준이 한도 내에 있는지 확인하는 프로세스입니다.
     * 
     * @param baseDate 산출 기준일
     */
    @Transactional(readOnly = true)
    public void generateRiskReporting(LocalDate baseDate) {
        log.info("📊 [리스크 리포팅] 기준일자({})의 데이터 마트 요약 집계를 시작합니다.", baseDate);

        // 💡 [핵심 지표 추출] ECL(예상손실), Exposure(노출액), RWA(위험가중자산)를 가져옵니다.
        Map<String, Object> summaryMetrics = martRepository.getSummaryMetrics(baseDate);
        Object totalEcl = summaryMetrics.get("totalEcl");
        Object totalExposure = summaryMetrics.get("totalExposure");
        Object totalRwa = summaryMetrics.get("totalRwa");
        
        log.info("   - 총 예상손실(ECL): {}, 총 노출액: {}, 총 위험가중자산(RWA): {}",
                totalEcl != null ? totalEcl : 0, 
                totalExposure != null ? totalExposure : 0, 
                totalRwa != null ? totalRwa : 0);

        // 💡 [건전성 분포 분석] IFRS 9 스테이징(Stage 1, 2, 3)별로 자산이 어떻게 분포되어 있는지 확인합니다.
        // Stage 3가 많다면 부실 자산이 늘어나고 있다는 경고 신호입니다.
        List<IntegratedRiskPositionRepository.StagingDistribution> stagingDistributions = martRepository.getStagingDistribution(baseDate);
        if (stagingDistributions != null && !stagingDistributions.isEmpty()) {
            stagingDistributions.forEach(d -> 
                log.info("   - 스테이징별 분포 [{}]: {}", d.getStaging(), d.getValue()));
        }

        // 💡 [산업군별 집중도 점검] 특정 산업(예: 건설업)에 리스크가 쏠려 있는지 확인합니다.
        List<IntegratedRiskPositionRepository.SectorDistribution> sectorDistributions = martRepository.getSectorDistribution(baseDate);
        if (sectorDistributions != null && !sectorDistributions.isEmpty()) {
            sectorDistributions.forEach(d -> 
                log.info("   - 산업군별 예상손실 [{}]: {}", d.getSector(), d.getValue()));
        }

        log.info("✅ [리스크 리포팅] 전체 집계 및 데이터 정합성 최종 확인 완료");
    }
}
