package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.AllowanceModelParameterRepository;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.model.AllowanceModelParameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [Service] IFRS 9 대손충당금 모델 파라미터 로딩 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AllowanceParameterService {

    private final AllowanceModelParameterRepository parameterRepository;
    private AllowanceModelParams cachedParams;

    /**
     * DB에서 최신 모델 파라미터를 로드하여 캐시를 갱신합니다.
     */
    public void refreshCache() {
        log.info("📦 [Allowance] DB에서 최신 대손충당금 모델 파라미터를 로드합니다.");
        Map<String, BigDecimal> paramMap = parameterRepository.findAll().stream()
                .collect(Collectors.toMap(
                        AllowanceModelParameter::getParamKey,
                        AllowanceModelParameter::getParamValue
                ));

        validateModelParams(paramMap);

        this.cachedParams = AllowanceModelParams.builder()
                .pdFloor(paramMap.get("PD_FLOOR"))
                .securedLgdFloor(paramMap.get("SECURED_LGD_FLOOR"))
                .unsecuredLgdFloor(paramMap.get("UNSECURED_LGD_FLOOR"))
                .defaultDiscountRate(paramMap.getOrDefault("DEFAULT_DISCOUNT_RATE", new BigDecimal("0.05")))
                .build();

        log.info("✅ [Allowance] 모델 파라미터 로드 완료 (PD Floor: {}, Unsecured LGD Floor: {})",
                cachedParams.getPdFloor(), cachedParams.getUnsecuredLgdFloor());
    }

    private void validateModelParams(Map<String, BigDecimal> paramMap) {
        String[] requiredKeys = {"PD_FLOOR", "SECURED_LGD_FLOOR", "UNSECURED_LGD_FLOOR"};

        for (String key : requiredKeys) {
            BigDecimal value = paramMap.get(key);
            if (value == null) {
                log.error("❌ [Allowance] 필수 모델 파라미터 '{}'가 DB에 존재하지 않습니다.", key);
                throw new IllegalStateException("필수 모델 파라미터 누락: " + key);
            }
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ [Allowance] 모델 파라미터 '{}'의 값이 0 이하입니다: {}", key, value);
                throw new IllegalArgumentException("유효하지 않은 모델 파라미터 값: " + key);
            }
        }
    }

    public AllowanceModelParams getParameters() {
        if (cachedParams == null) {
            refreshCache();
        }
        return cachedParams;
    }
}
