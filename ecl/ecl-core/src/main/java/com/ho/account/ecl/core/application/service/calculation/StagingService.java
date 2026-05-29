package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrGradeMasterRepository;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.model.CrGradeMaster;
import com.ho.account.shared.finance.enums.CrStaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * [Service] [IFRS 9] 자산 건전성 스테이징(Staging) 서비스.
 * 연체 일수, 신용 등급 변동, 조기경보 신호 등을 종합하여 자산을 3단계로 분류합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * IFRS 9 (국제회계기준)에서는 은행이 보유한 대출 자산을 다음 3단계로 나누어 관리하도록 합니다.
 * 각 단계에 따라 미래 기대손실(ECL)을 다르게 산출하기 때문에, 정확한 스테이징이 매우 중요합니다.
 *
 * 1. Stage 1 (정상): 신용 위험이 크게 변하지 않은 상태.
 * 이 서비스는 대출 계좌의 '건강 등급'을 매기는 의사와 같습니다.
 * IFRS 9이라는 회계 기준에 따라 자산의 상태를 정상(Stage 1), 주의(Stage 2), 손상(Stage 3)으로 분류하며,
 * 이 분류 결과에 따라 나중에 은행이 쌓아야 할 충담금(ECL)의 양이 크게 달라집니다.
 * 
 * 판정 원칙:
 * 1. Stage 3 (손상): 90일 이상 연체되거나 채무 조정을 받은 '부도' 상태입니다.
 * 2. Stage 2 (주의): 신용 위험이 예전보다 크게 높아진 상태입니다. (예: 30일 연체, 내부 등급 급락 등)
 * 3. Stage 1 (정상): 위의 위험 징후가 없는 깨끗한 상태입니다.
 *
 * 💡 [핵심 용어 설명]
 * - ECL (Expected Credit Loss): 미래 기대손실. 은행이 대출을 회수하지 못할 것으로 예상되는 금액입니다.
 * - PD (Probability of Default): 부도 확률. 고객이 1년 내에 부도가 날 확률입니다.
 * - LGD (Loss Given Default): 부도 시 손실률. 고객이 부도났을 때, 담보 등을 제외하고 실제로 잃게 되는 비율입니다.
 *
 * 🔧 [v2.1 고도화 내역]
 * - 기존: getRatingRank()에 20개 등급이 switch 문으로 하드코딩
 * - 문제: 등급 체계 개편(20등급→22등급) 시 코드 수정 필요
 * - 변경: {@code cr_grade_masters} 테이블의 notch_order 값을 DB에서 조회 + 캐싱
 * - 효과: 등급 체계 변경 시 DB만 업데이트하면 됩니다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StagingService {

    /** 💡 [초보자 가이드] 등급 마스터 테이블에서 각 신용 등급의 '순위(Notch Order)'를 가져오는 통로입니다. */
    private final CrGradeMasterRepository gradeMasterRepository;

    // ==========================================
    // 1. 등급 순서(Notch Order) 캐시
    // ==========================================

    /**
     * 💡 등급 체계가 변경되어도 DB의 cr_grade_masters.notch_order만 수정하면
     *    자동으로 반영됩니다. 코드 변경이 필요 없습니다.
     */
    private final Map<String, Integer> ratingRankCache = new ConcurrentHashMap<>();

    /** 캐시 초기화 완료 여부 */
    private volatile boolean rankCacheLoaded = false;

    // ==========================================
    // 2. 스테이징 판정 공개 API
    // ==========================================

    /**
     * 계좌의 현재 상태를 바탕으로 IFRS 9 단계를 결정합니다.
     *
     * 💡 [판정 순서 (우선순위 높은 것부터)]
     * ① Stage 3: 연체 90일 이상 또는 채권재조정 상태
     * ② Stage 2: 연체 30일 이상, 조기경보 CRITICAL, 등급 3노치 이상 하락, 조기경보 WARNING
     * ③ Stage 1: 위 조건에 해당하지 않는 정상 상태
     *
     * @param account            대상 계좌
     * @param warningLevel       조기경보 등급 (null, "WARNING", "CRITICAL")
     * @param isDebtRestructured 채권재조정 여부
     * @return 결정된 IFRS 9 단계 (STAGE1, STAGE2, STAGE3)
     */
    public CrStaging determineStage(CrAccount account, String warningLevel, boolean isDebtRestructured) {
        int delinquentDays = (account.getDelinquentDays() != null) ? account.getDelinquentDays() : 0;

        // ──────────────────────────────────────────
        // 1. Stage 3 (손상) 판정
        // 💡 연체 90일 이상이면 IFRS 9 모델 기준 "부도(Default)"로 간주합니다.
        //    채권재조정(Debt Restructured)은 이미 상환에 문제가 있어 조건을 변경한 것이므로
        //    역시 손상 자산으로 분류합니다.
        // ──────────────────────────────────────────
        if (delinquentDays >= 90 || isDebtRestructured) {
            log.info("🔎 [스테이징] Stage 3 결정: 계좌 {} (연체일수 {}일, 채권재조정 {})",
                    account.getAccountNo(), delinquentDays, isDebtRestructured);
            return CrStaging.STAGE3;
        }

        // ──────────────────────────────────────────
        // 2. Stage 2 (위험 증가, SICR) 판정
        // 💡 SICR = Significant Increase in Credit Loss (신용위험의 유의미한 증가)
        //    아래 조건 중 하나라도 해당하면 "위험이 크게 증가했다"고 판단합니다.
        // ──────────────────────────────────────────

        // 2-1. 연체 30일 이상 또는 조기경보 '심각(CRITICAL)' 단계
        if (delinquentDays >= 30 || "CRITICAL".equals(warningLevel)) {
            log.info("🔎 [스테이징] Stage 2 결정: 계좌 {} (SICR 신호 - 연체일수 {}일, 조기경보 {})",
                    account.getAccountNo(), delinquentDays, warningLevel);
            return CrStaging.STAGE2;
        }

        // 2-2. 등급 유의미 하락 판정 (3 Notch 이상 하락)
        // 💡 대출 실행 시 BBB 등급이었던 고객이 현재 B 등급으로 떨어졌다면,
        //    신용 위험이 크게 증가한 것이므로 Stage 2로 분류합니다.
        if (isSignificantRatingDowngrade(account)) {
            log.info("🔎 [스테이징] Stage 2 결정: 계좌 {} (등급 유의미 하락 - 3 Notch 이상)", account.getAccountNo());
            return CrStaging.STAGE2;
        }

        // 2-3. 조기경보 '주의(WARNING)' 단계
        if ("WARNING".equals(warningLevel)) {
            log.info("🔎 [스테이징] Stage 2 결정: 계좌 {} (조기경보 WARNING 상태)", account.getAccountNo());
            return CrStaging.STAGE2;
        }

        // ──────────────────────────────────────────
        // 3. Stage 1 (정상)
        // 💡 위 모든 조건에 해당하지 않으면 정상 자산입니다.
        // ──────────────────────────────────────────
        return CrStaging.STAGE1;
    }

    // ==========================================
    // 3. 등급 하락 판정 (내부 로직)
    // ==========================================

    /**
     * 실행 시점 등급과 현재 등급을 비교하여 유의미하게 하락했는지 판정합니다.
     *
     * 💡 [3 Notch 규칙]
     * 등급 순서(notchOrder)의 차이가 3 이상이면 "유의미한 하락"으로 판정합니다.
     * 예: BBB(9) → B(15) = 차이 6 → 유의미 하락
     *     AAA(1) → AA(3) = 차이 2 → 유의미하지 않음
     *
     * @param account 대상 계좌 (originalRating과 현재 등급을 비교)
     * @return true이면 3 Notch 이상 하락한 것
     */
    private boolean isSignificantRatingDowngrade(CrAccount account) {
        if (account.getOriginalRating() == null) {
            return false;
        }

        // [v2.1] DB 기반 등급 순서 조회 (기존: switch 하드코딩 → 변경: cr_grade_masters.notch_order)
        int originalRank = getRatingRank(account.getOriginalRating());
        int currentRank = getRatingRank(account.getCustomer().getInternalRating());

        // 랭크 숫자가 클수록 낮은 등급 (예: AAA=1, D=20)
        // 차이가 3 이상이면 유의미한 하락
        return (currentRank - originalRank) >= 3;
    }

    /**
     * [v2.1 고도화] 등급 코드의 Notch 순서를 반환합니다.
     *
     * 💡 [기존 방식 vs 변경 방식]
     * - 기존: switch("AAA"→1, "AA+"→2, ..., "D"→20) 하드코딩 (20개)
     *   → 등급 체계 개편 시 코드 수정 필요
     * - 변경: cr_grade_masters 테이블의 notch_order 컬럼에서 조회 + 캐싱
     *   → DB만 수정하면 됩니다 (예: 20등급→22등급 전환 시 DB에 2행 추가)
     *
     * @param rating 등급 코드 (예: "AAA", "BB+")
     * @return 순서 번호 (1=최우량 ~ 20=부도). 등급이 없으면 10(중간값) 반환.
     */
    private int getRatingRank(String rating) {
        if (rating == null) {
            return 10; // 등급 정보 없음 → 보수적 중간값 가정
        }

        // 캐시 초기 로드
        ensureRankCacheLoaded();

        String normalizedRating = rating.toUpperCase();
        return ratingRankCache.getOrDefault(normalizedRating, 10);
    }

    // ==========================================
    // 4. 캐시 관리
    // ==========================================

    /**
     * 등급 순서 캐시가 아직 로드되지 않았으면 DB에서 로드합니다.
     *
     * 💡 Double-Checked Locking 패턴으로 멀티 스레드 안전성을 보장합니다.
     *    배치 환경에서 여러 스레드가 동시에 호출해도 DB는 딱 한 번만 조회됩니다.
     */
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

    /**
     * DB에서 전체 등급 마스터를 조회하여 notch_order 캐시에 적재합니다.
     */
    private void loadRankCacheFromDatabase() {
        List<CrGradeMaster> allGrades = gradeMasterRepository.findAll();

        for (CrGradeMaster grade : allGrades) {
            ratingRankCache.put(grade.getRatingCode().toUpperCase(), grade.getNotchOrder());
        }

        log.info("📦 [스테이징] DB에서 {}개의 등급 순서(Notch Order)를 캐시에 로드했습니다.", allGrades.size());
    }

    /**
     * 등급 순서 캐시를 강제 갱신합니다.
     *
     * 💡 사용 시나리오: 은행의 등급 체계가 개편된 후 호출하세요.
     *    예: 기존 20등급 체계에서 22등급 체계로 전환 시,
     *    DB에 신규 등급 2개를 추가하고 이 메서드를 호출하면 즉시 반영됩니다.
     */
    public void refreshRankCache() {
        log.info("📦 [스테이징] 등급 순서 캐시 갱신 요청");
        synchronized (this) {
            ratingRankCache.clear();
            loadRankCacheFromDatabase();
        }
        log.info("✅ [스테이징] 등급 순서 캐시 갱신 완료: 총 {}개 등급", ratingRankCache.size());
    }
}


