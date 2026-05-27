package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrProductMasterRepository;
import com.ho.account.ecl.core.domain.model.CrProductMaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Credit Core] CCF(신용환산계수) 산출 전문 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * CCF (Credit Conversion Factor)란 '아직 빌려주지 않은 돈이 미래에 대출로 바뀔 확률'을 말합니다.
 * 
 * 예시: 
 * 고객이 1억 원짜리 마이너스 통장을 만들고 2천만 원만 썼다면, 남은 8천만 원은 아직 은행 돈입니다. 
 * 하지만 고객이 언제든 뽑아 쓸 수 있으므로, 은행은 이 8천만 원도 리스크가 있다고 보고 
 * 일정 비율(예: 75%)을 곱해서 '가상의 대출금'으로 간주하고 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CcfCalculationService {

    /** 💡 [초보자 가이드] 상품별로 정해진 규제 CCF 비율을 찾기 위한 마스터 저장소입니다. */
    private final CrProductMasterRepository productMasterRepository;
    
    /** 💡 [초보자 가이드] 수백만 건의 연산 속도를 높이기 위해, 상품별 CCF를 메모리에 미리 담아두는 바구니입니다. */
    private final ConcurrentHashMap<String, BigDecimal> ccfCache = new ConcurrentHashMap<>();

    /**
     * 전사 상품 마스터의 CCF 비율을 메모리에 캐싱합니다.
     * 💡 상품 정보는 배치 실행 중에는 거의 변하지 않으므로, 시작 시점에 단 한 번 로드하여
     *    수백만 건의 대량 연산 시 DB 부하를 원천 차단합니다.
     */
    public void refreshCache() {
        log.info("📦 [CCF 산출] 상품 마스터로부터 CCF 캐시 갱신 중...");
        productMasterRepository.findAll().forEach(product -> 
            ccfCache.put(product.getProductCode().toUpperCase(), product.getCcfRate())
        );
        log.info("✅ [CCF 산출] 총 {}개의 상품 CCF 정보가 캐싱되었습니다.", ccfCache.size());
    }

    /**
     * 상품 마스터 정보를 기반으로 규제 준수 CCF를 조회합니다.
     *
     * 💡 [비즈니스 시뮬레이션]
     * 일반 신용대출은 이미 돈이 다 나갔으므로 CCF가 의미 없지만, 
     * '유동성 라인'이나 '약정 대출'은 고객이 언제든 돈을 찾을 수 있는 권리가 있습니다.
     * 따라서 상품 코드별로 사전에 정의된 규제 비율(CCF)을 적용합니다.
     *
     * @param productCode 상품 코드 (마스터 조회 키)
     * @return 상품별 규제 CCF 비율 (0.0 ~ 1.0)
     */
    public BigDecimal calculateCcf(String productCode) {
        if (productCode == null) return new BigDecimal("0.75");

        // 💡 [최적화] Repository 직접 조회 대신 메모리에 로드된 ccfCache를 활용합니다.
        BigDecimal cachedCcf = ccfCache.get(productCode.toUpperCase());
        
        if (cachedCcf != null) {
            return cachedCcf;
        }

        // 캐시에 없는 경우 (실시간 조회 Fallback)
        log.warn("⚠️ [CCF 산출] 상품코드 '{}'의 캐시를 찾을 수 없습니다. 직접 조회 시도.", productCode);
        return productMasterRepository.findByProductCode(productCode)
                .map(CrProductMaster::getCcfRate)
                .orElseGet(() -> {
                    log.warn("⚠️ [CCF 산출] 상품코드 '{}'의 CCF 설정을 찾을 수 없습니다. 기본 규제 비율(75%) 적용", productCode);
                    return new BigDecimal("0.75");
                });
    }
}
