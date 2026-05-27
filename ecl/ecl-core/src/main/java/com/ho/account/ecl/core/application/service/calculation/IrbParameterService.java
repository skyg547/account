package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrRegulatoryParameterRepository;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.model.CrRegulatoryParameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [Credit Core] IRB 규제 파라미터 로딩 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * IRB(내부등급법) 리스크 계산기에는 법으로 정해진 수많은 '규제 상수(Fixed Numbers)'들이 들어갑니다.
 * 예: "PD는 아무리 낮아도 0.03%로 해라(PD Floor)", "부동산 담보 대출은 손실률을 최소 10%로 잡아라" 등입니다.
 * 이 서비스는 DB에 저장된 이러한 '규제 규칙(파라미터)'들을 읽어와서 시스템 전역에서 사용할 수 있게 해주는 '규정 관리자' 역할을 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IrbParameterService {

    /** 💡 [초보자 가이드] 규제 파라미터(PD Floor, LGD Floor 등) 정보가 담긴 DB 테이블에 접근하는 통로입니다. */
    private final CrRegulatoryParameterRepository parameterRepository;

    /** 💡 [초보자 가이드] 수천 만 건의 계산 중에 매번 DB를 읽으면 느려지므로, 파라미터들을 묶어서 한 번에 메모리에 저장해두는 바구니(캐시)입니다. */
    private IrbRegulatoryParams cachedParams;

    /**
     * DB에서 최신 규제 파라미터를 로드하여 캐시를 갱신합니다.
     * 
     * 💡 [중요] 필수 규제 키(PD_FLOOR 등)가 누락되었거나 값이 0 이하인 경우, 
     *    잘못된 대손충당금(IFRS9) 산출이 일어나는 것을 막기 위해 시스템을 멈추고 에러를 발생시킵니다(Self-Healing 및 무결성 보장).
     */
    public void refreshCache() {
        log.info("📦 [규제 파이프라인] DB에서 최신 IRB 규제 파라미터를 로드합니다.");
        Map<String, BigDecimal> paramMap = parameterRepository.findAll().stream()
                .collect(Collectors.toMap(
                        CrRegulatoryParameter::getParamKey,
                        CrRegulatoryParameter::getParamValue
                ));

        // 1. 필수 파라미터 존재 여부 및 값의 유효성 검증
        // 💡 [초보자 가이드] 재무 결산 시스템에서는 '기본값'보다 '정확한 규제값'이 중요하므로 엄격하게 검증합니다.
        validateRegulatoryParams(paramMap);

        // 💡 [데이터 변환] DB에서 가져온 Key-Value 쌍을 시스템이 이해하기 쉬운 VO(IrbRegulatoryParams) 객체로 옮겨 담습니다.
        this.cachedParams = IrbRegulatoryParams.builder()
                .pdFloor(paramMap.get("PD_FLOOR"))
                .assetCorrBase(paramMap.getOrDefault("ASSET_CORR_BASE", new BigDecimal("0.12")))
                .assetCorrHigh(paramMap.getOrDefault("ASSET_CORR_HIGH", new BigDecimal("0.24")))
                .retailCorrBase(paramMap.getOrDefault("RETAIL_CORR_BASE", new BigDecimal("0.03")))
                .retailCorrHigh(paramMap.getOrDefault("RETAIL_CORR_HIGH", new BigDecimal("0.16")))
                .maturityConst1(paramMap.getOrDefault("MATURITY_ADJ_CONST", new BigDecimal("0.11852")))
                .maturityConst2(paramMap.getOrDefault("MATURITY_ADJ_CONST2", new BigDecimal("0.05478")))
                .smeSizeThreshold(paramMap.getOrDefault("SME_SIZE_THRESHOLD", new BigDecimal("50")))
                .securedLgdFloor(paramMap.get("SECURED_LGD_FLOOR"))
                .unsecuredLgdFloor(paramMap.get("UNSECURED_LGD_FLOOR"))
                .fiCorrMultiplier(paramMap.getOrDefault("FI_CORR_MULTIPLIER", new BigDecimal("1.25")))
                .fiCorrCap(paramMap.getOrDefault("FI_CORR_CAP", new BigDecimal("0.30")))
                .smeSizeMin(paramMap.getOrDefault("SME_SIZE_MIN", new BigDecimal("5")))
                .defaultDiscountRate(paramMap.getOrDefault("DEFAULT_DISCOUNT_RATE", new BigDecimal("0.05")))
                .build();
        
        log.info("✅ [규제 파이프라인] IRB 파라미터 로드 완료 (PD Floor: {}, Unsecured LGD Floor: {})", 
                cachedParams.getPdFloor(), cachedParams.getUnsecuredLgdFloor());
    }

    /**
     * 규제 파라미터 셋의 무결성을 검증합니다.
     * @param paramMap DB에서 로드된 원시 파라미터 맵
     */
    private void validateRegulatoryParams(Map<String, BigDecimal> paramMap) {
        String[] requiredKeys = {"PD_FLOOR", "SECURED_LGD_FLOOR", "UNSECURED_LGD_FLOOR"};
        
        for (String key : requiredKeys) {
            BigDecimal value = paramMap.get(key);
            if (value == null) {
                log.error("❌ [규제 오류] 필수 파라미터 '{}'가 DB에 존재하지 않습니다.", key);
                throw new IllegalStateException("필수 규제 파라미터 누락: " + key);
            }
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ [규제 오류] 파라미터 '{}'의 값이 0 이하입니다: {}", key, value);
                throw new IllegalArgumentException("유효하지 않은 규제 파라미터 값: " + key);
            }
        }
    }

    /**
     * 캐싱된 규제 파라미터를 반환합니다. 캐시가 없으면(최초 호출 시) DB에서 로드합니다.
     * @return 로드된 규제 파라미터 객체
     */
    public IrbRegulatoryParams getParameters() {
        if (cachedParams == null) {
            refreshCache();
        }
        return cachedParams;
    }
}
