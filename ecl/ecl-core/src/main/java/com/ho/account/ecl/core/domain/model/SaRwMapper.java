package com.ho.account.ecl.core.domain.model;

import com.ho.account.shared.finance.enums.CustomerType;
import com.ho.account.ecl.core.application.port.out.CrSaRwMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * [Component] 국제 금융 규제 표준방법(SA) 위험가중치 매핑 서비스.
 *
 * 금융감독기관의 표준 가이드라인에 따라 차주 유형과 신용 등급의 조합으로
 * 표준방법 위험가중치(Risk Weight)를 결정합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 1. RW (Risk Weight, 위험가중치): 자산의 위험한 정도를 소수(0~2.5)로 표현한 것입니다.
 *    예를 들어 국채는 위험이 거의 없으므로 0.00, 일반 개인 대출은 0.75 등으로 정해져 있습니다.
 * 2. 표준방법 (SA): 은행이 자체 모델을 쓰지 않고, 규제당국이 미리 정해준 '표준 위험가중치'를
 *    단순 매핑하는 방식입니다.
 * 3. 차주 유형 (Customer Type): 돈을 빌린 사람이 개인(Retail)인지, 중소기업(SME)인지,
 *    대기업(Corporate)인지에 따라 위험가중치가 달라집니다.
 *
 * 🔧 [v2.1 고도화 내역]
 * - 기존: switch 문에 "AAA" → 0.20, "A+" → 0.50 등이 하드코딩되어 있었음
 * - 문제: 감독기관 가이드라인 변경 시 코드 수정 필요, 등급 체계 개편(20→22등급) 시 코드 수정 필요
 * - 변경: {@code cr_sa_rw_masters} 테이블에서 동적 조회 + 메모리 캐싱
 * - 효과: DB만 업데이트하면 즉시 반영, 코드 무수정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaRwMapper {

    private final CrSaRwMasterRepository saRwMasterRepository;

    // ==========================================
    // 1. 메모리 캐시
    // ==========================================

    /**
     * 위험가중치 캐시: Key = "차주유형::등급코드" (예: "CORPORATE::AAA")
     *
     * 💡 [캐싱 전략]
     * 배치 산출 중 수백만 건의 계좌를 처리할 때, 매 건마다 DB를 조회하면 성능이 떨어집니다.
     * 따라서 첫 조회 시 결과를 메모리에 저장해두고, 이후에는 캐시에서 바로 반환합니다.
     * ConcurrentHashMap을 사용하여 멀티 스레드 환경에서도 안전합니다.
     */
    private final Map<String, BigDecimal> rwCache = new ConcurrentHashMap<>();

    /**
     * 캐시 초기화 완료 여부.
     * 💡 초기 로드를 한 번만 수행하기 위한 플래그입니다.
     */
    private volatile boolean cacheLoaded = false;

    // ==========================================
    // 2. 공개 API
    // ==========================================

    /**
     * [고도화] 차주 유형과 신용 등급에 따라 표준방법 위험가중치를 반환합니다.
     *
     * 💡 [조회 우선순위]
     * ① 정확한 매칭: "CORPORATE" + "AAA" → 0.20
     * ② 차주 유형별 기본값: "CORPORATE" + "UNRATED" → 1.00
     * ③ 전역 기본값: 1.00 (100%)
     *
     * @param customerType 차주 유형 (개인, 중소기업, 법인 등)
     * @param rating       익스포저 신용 등급 (AAA, BBB 등)
     * @return 대손충당금(IFRS9) 위험가중치(RW)
     */
    public BigDecimal getStandardRw(CustomerType customerType, String rating) {
        if (customerType == null) {
            log.warn("⚠️ [SA RW] 차주 유형이 null입니다. 기본 RW 100%를 적용합니다.");
            return new BigDecimal("1.00");
        }

        // 캐시 초기 로드 (최초 호출 시 1회만)
        ensureCacheLoaded();

        String typeKey = customerType.name();
        String ratingKey = (rating != null) ? rating.toUpperCase() : "UNRATED";

        // ① 정확한 매칭 시도: "차주유형::등급코드"
        String exactKey = typeKey + "::" + ratingKey;
        BigDecimal rw = rwCache.get(exactKey);
        if (rw != null) {
            return rw;
        }

        // ② "ALL" 등급 조회 (해당 차주 유형의 모든 등급에 공통 적용되는 가중치)
        // 💡 소매(RETAIL), SME 등은 등급과 무관하게 동일 RW를 적용하므로
        //    DB에 "RETAIL::ALL → 0.75"로 등록해 놓습니다.
        String allKey = typeKey + "::ALL";
        rw = rwCache.get(allKey);
        if (rw != null) {
            return rw;
        }

        // ③ "UNRATED" 조회 (해당 차주 유형의 무등급 기본값)
        String unratedKey = typeKey + "::UNRATED";
        rw = rwCache.get(unratedKey);
        if (rw != null) {
            return rw;
        }

        // ④ 최종 폴백: 가장 보수적인 100%
        log.warn("⚠️ [SA RW] 매핑을 찾을 수 없습니다: 차주유형={}, 등급={}. 기본 RW 100% 적용", typeKey, ratingKey);
        return new BigDecimal("1.00");
    }

    /**
     * 캐시를 강제로 갱신합니다.
     *
     * 💡 사용 시나리오:
     * - 감독기관이 SA 위험가중치 가이드라인을 변경한 경우
     * - 은행의 등급 체계가 개편된 경우 (예: 20등급 → 22등급)
     * - DB에 신규 매핑 데이터를 추가한 후 즉시 반영이 필요한 경우
     */
    public void refreshCache() {
        log.info("📦 [SA RW] 캐시 갱신 요청 - DB에서 최신 SA 위험가중치 매핑을 다시 로드합니다.");
        synchronized (this) {
            rwCache.clear();
            loadCacheFromDatabase();
        }
        log.info("✅ [SA RW] 캐시 갱신 완료: 총 {}개의 매핑 로드됨", rwCache.size());
    }

    // ==========================================
    // 3. 캐시 로딩 로직
    // ==========================================

    /**
     * 캐시가 로드되지 않았으면 DB에서 로드합니다 (Double-Checked Locking).
     */
    private void ensureCacheLoaded() {
        if (!cacheLoaded) {
            synchronized (this) {
                if (!cacheLoaded) {
                    loadCacheFromDatabase();
                    cacheLoaded = true;
                }
            }
        }
    }

    /**
     * DB에서 전체 SA RW 매핑을 조회하여 캐시에 적재합니다.
     *
     * 💡 [동작 원리]
     * 1. cr_sa_rw_masters 테이블의 모든 행을 조회합니다.
     * 2. 각 행을 "차주유형::등급코드" → 위험가중치 형태로 캐시 맵에 저장합니다.
     * 3. 이후 getStandardRw() 호출 시 DB 대신 캐시에서 즉시 반환합니다.
     */
    private void loadCacheFromDatabase() {
        List<CrSaRwMaster> allMappings = saRwMasterRepository.findAll();

        for (CrSaRwMaster mapping : allMappings) {
            String key = mapping.getCustomerType() + "::" + mapping.getRatingCode();
            rwCache.put(key, mapping.getRiskWeight());
        }

        log.info("📦 [SA RW] DB에서 {}개의 위험가중치 매핑을 캐시에 로드했습니다.", allMappings.size());
    }
}
