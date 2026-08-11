package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrGradeMasterRepository;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.calculator.PdCalculator;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.model.CrGradeMaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [애플리케이션 서비스] PD(부도확률) 산출 조율 서비스 (PD Calculation Application Service)
 * 
 * 💡 [DDD 설계 원칙]
 * 본 애플리케이션 서비스는 신용 등급 마스터 및 캐시 데이터를 로드하고,
 * Pure Domain Calculator인 {@link PdCalculator}를 호출하여 동적 할증 및 Floor가 적용된 PD 수치를 산출하도록 프로세스를 조율합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdCalculationService {

    /** 신용 등급별 부도 확률(PD) 데이터 포트 */
    private final CrGradeMasterRepository gradeMasterRepository;
    
    /** Pure Domain Calculator */
    private final PdCalculator pdCalculator;

    /** 대량 계산 시 성능 향상을 위한 메모리 캐시 */
    private final Map<String, BigDecimal> pdCache = new ConcurrentHashMap<>();

    /**
     * 전체 신용 등급의 PD 값을 DB에서 읽어와 메모리(pdCache)에 적재합니다.
     */
    public void refreshCache() {
        log.info("📦 [PD 산출] 등급 마스터로부터 PD 캐시 갱신 중...");
        gradeMasterRepository.findAll().forEach(grade -> 
            pdCache.put(grade.getRatingCode().toUpperCase(), grade.getPdValue())
        );
        log.info("✅ [PD 산출] 총 {}개의 등급 PD 정보가 캐싱되었습니다.", pdCache.size());
    }

    /**
     * 계좌 및 차주 정보를 기반으로 Pure Domain Calculator를 기동하여 최종 PD를 산출합니다.
     *
     * @param account   산출 대상 계좌
     * @param modelParams 모델 파라미터
     * @return 보정 및 Floor가 적용된 최종 PD
     */
    public BigDecimal calculatePd(CrAccount account, AllowanceModelParams modelParams) {
        CrCustomer customer = account.getCustomer();
        
        // 1. 등급 결정 (계좌 등급 우선 > 차주 등급)
        String resolvedRating = (account.getInternalRating() != null) 
                ? account.getInternalRating() 
                : customer.getInternalRating();

        // 2. 기초 PD 조회 (포트/캐시 연동)
        BigDecimal basePd = getBasePdFromCache(resolvedRating, modelParams.getPdFloor());

        // 3. Pure Domain Calculator 위임 호출 (동적 PD 할증 및 Floor 반영)
        BigDecimal finalPd = pdCalculator.calculateAdjusted12MonthPd(
                basePd,
                modelParams.getPdFloor(),
                account.getDelinquentDays(),
                customer.getWarningLevel()
        );
        
        log.debug("📊 [PD 산출 최종] 계좌번호: {}, 적용등급: {}, 최종 PD: {}", 
                account.getAccountNo(), resolvedRating, finalPd);
        return finalPd;
    }

    /**
     * 캐시에서 PD 값을 안전하게 꺼내옵니다.
     */
    private BigDecimal getBasePdFromCache(String ratingCode, BigDecimal pdFloor) {
        if (ratingCode == null) return pdFloor;
        
        if (pdCache.isEmpty()) {
            log.warn("⚠️ [PD 캐시] 캐시가 비어있습니다. Repository에서 직접 조회합니다.");
            return gradeMasterRepository.findByRatingCode(ratingCode)
                    .map(CrGradeMaster::getPdValue)
                    .orElse(pdFloor);
        }

        BigDecimal pdValue = pdCache.get(ratingCode.toUpperCase());
        if (pdValue == null) {
            log.warn("⚠️ [PD 캐시] 등급 '{}' 에 대한 PD 값이 정의되지 않았습니다. PD Floor 적용.", ratingCode);
            return pdFloor;
        }
        return pdValue;
    }
}



