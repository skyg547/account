package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrGradeMasterRepository;
import com.ho.account.ecl.core.domain.calculator.IfrsStagingEngine;
import com.ho.account.ecl.core.domain.calculator.StagingDecisionResult;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.model.CrGradeMaster;
import com.ho.account.shared.finance.enums.CrStaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Service] [IFRS 9] 자산 건전성 스테이징(Staging) 서비스.
 * 연체 일수, 신용 등급 변동, 조기경보 신호 등을 종합하여 자산을 3단계로 분류합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * IFRS 9 (국제회계기준)에서는 은행이 보유한 대출 자산을 다음 3단계로 나누어 관리하도록 합니다.
 * 각 단계에 따라 미래 기대손실(ECL)을 다르게 산출하기 때문에, 정확한 스테이징이 매우 중요합니다.
 *
 * 1. Stage 1 (정상): 신용 위험이 크게 변하지 않은 상태 (12개월 ECL)
 * 2. Stage 2 (주의/SICR): 신용 위험이 예전보다 크게 높아진 상태 (Lifetime ECL)
 * 3. Stage 3 (손상/Default): 90일 이상 연체되거나 채무 조정을 받은 부도 상태 (Lifetime ECL)
 *
 * 🔧 [v2.2 고도화 내역]
 * - 도메인 규칙 분리: 순수 도메인 계산기 {@link IfrsStagingEngine}으로 스테이징 평가 규칙 위임
 * - 세부 이력 지원: {@link StagingDecisionResult}로 결정 트리거 및 메트릭 상세 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StagingService {

    /** 💡 [초보자 가이드] 등급 마스터 테이블에서 각 신용 등급의 '순위(Notch Order)'를 가져오는 통로입니다. */
    private final CrGradeMasterRepository gradeMasterRepository;

    /** 💡 [v2.2] 순수 도메인 IFRS 9 스테이징 계산 엔진 */
    private final IfrsStagingEngine stagingEngine;

    // ==========================================
    // 1. 등급 순서(Notch Order) 캐시
    // ==========================================

    private final Map<String, Integer> ratingRankCache = new ConcurrentHashMap<>();
    private volatile boolean rankCacheLoaded = false;

    // ==========================================
    // 2. 스테이징 판정 공개 API
    // ==========================================

    /**
     * 계좌의 현재 상태를 바탕으로 IFRS 9 단계를 결정합니다.
     *
     * @param account            대상 계좌
     * @param warningLevel       조기경보 등급 (null, "WARNING", "CRITICAL")
     * @param isDebtRestructured 채권재조정 여부
     * @return 결정된 IFRS 9 단계 (STAGE1, STAGE2, STAGE3)
     */
    public CrStaging determineStage(CrAccount account, String warningLevel, boolean isDebtRestructured) {
        StagingDecisionResult result = determineStageDetailed(account, warningLevel, isDebtRestructured);
        return result.getStage();
    }

    /**
     * 계좌의 현재 상태를 바탕으로 IFRS 9 단계 및 세부 트리거 사유를 포함한 판정 결과를 산출합니다.
     *
     * @param account            대상 계좌
     * @param warningLevel       조기경보 등급
     * @param isDebtRestructured 채권재조정 여부
     * @return 상세 스테이징 판정 결과
     */
    public StagingDecisionResult determineStageDetailed(CrAccount account, String warningLevel, boolean isDebtRestructured) {
        int delinquentDays = (account.getDelinquentDays() != null) ? account.getDelinquentDays() : 0;
        int originalRank = getRatingRank(account.getOriginalRating());
        
        String currentRating = (account.getCustomer() != null && account.getCustomer().getInternalRating() != null)
                ? account.getCustomer().getInternalRating()
                : account.getInternalRating();
        int currentRank = getRatingRank(currentRating);

        StagingDecisionResult decision = stagingEngine.evaluateStaging(
                delinquentDays,
                originalRank,
                currentRank,
                warningLevel,
                isDebtRestructured
        );

        log.info("🔎 [스테이징] 계좌 {} 판정 결과: Stage={}, Trigger={}, DPD={}일, NotchDowngrade={}",
                account.getAccountNo(), decision.getStage(), decision.getPrimaryTrigger(),
                decision.getDelinquentDays(), decision.getNotchDowngrade());

        return decision;
    }

    // ==========================================
    // 3. 등급 순서(Notch Order) 조회
    // ==========================================

    public int getRatingRank(String rating) {
        if (rating == null) {
            return 10; // 등급 정보 없음 → 보수적 중간값 가정
        }

        ensureRankCacheLoaded();
        String normalizedRating = rating.toUpperCase();
        return ratingRankCache.getOrDefault(normalizedRating, 10);
    }

    // ==========================================
    // 4. 캐시 관리
    // ==========================================

    private void ensureRankCacheLoaded() {
        if (!rankCacheLoaded) {
            synchronized (this) {
                if (!rankCacheLoaded) {
                    loadRankCacheFromDatabase();
                    rankCacheLoaded = true;
                }
            }
        }
    }

    private void loadRankCacheFromDatabase() {
        List<CrGradeMaster> allGrades = gradeMasterRepository.findAll();
        for (CrGradeMaster grade : allGrades) {
            ratingRankCache.put(grade.getRatingCode().toUpperCase(), grade.getNotchOrder());
        }
        log.info("📦 [스테이징] DB에서 {}개의 등급 순서(Notch Order)를 캐시에 로드했습니다.", allGrades.size());
    }

    public void refreshRankCache() {
        log.info("📦 [스테이징] 등급 순서 캐시 갱신 요청");
        synchronized (this) {
            ratingRankCache.clear();
            loadRankCacheFromDatabase();
        }
        log.info("✅ [스테이징] 등급 순서 캐시 갱신 완료: 총 {}개 등급", ratingRankCache.size());
    }
}
