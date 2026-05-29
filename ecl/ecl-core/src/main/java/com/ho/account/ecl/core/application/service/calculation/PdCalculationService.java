package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrGradeMasterRepository;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
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
 * [백엔드] PD(부도확률) 산출 전문 서비스 (PD Calculation Service)
 * [Credit Core] PD(부도확률) 산출 전문 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * PD (Probability of Default)란 고객이 돈을 갚지 못하고 '부도(망함)'를 낼 확률(0~100%)을 말합니다.
 * 은행은 모든 고객에게 '신용 등급'을 매기고, 그 등급에 따라 "과거 통계상 이 등급 고객은 내년에 몇 %나 망했는가"를 수치로 관리합니다.
 * 
 * IFRS 9 모델 관점:
 * - PD는 아무리 신용이 좋아도 금융당국이 정한 최저 한도(PD Floor, 예: 0.03%)를 밑돌 수 없습니다. 
 * - 만약 고객이 연체 중이라면, 시스템은 이 PD를 평소보다 높게 올려서 보수적으로 대손충당금을 보수적으로 산출합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdCalculationService {

    /** 💡 [초보자 가이드] 신용 등급별 부도 확률(PD) 데이터에 접근하는 리포지토리입니다. */
    private final CrGradeMasterRepository gradeMasterRepository;
    
    /** 💡 [초보자 가이드] 대량 계산 시 속도를 높이기 위해, DB 내용을 메모리에 미리 담아놓는 저장소(캐시)입니다. */
    private final Map<String, BigDecimal> pdCache = new ConcurrentHashMap<>();

    /**
     * 전체 신용 등급의 PD 값을 DB에서 읽어와 메모리(pdCache)에 적재합니다.
     * 💡 [v2.0] 배치 전 캐시 워밍업을 통해 대량 산출 시 DB IO 부하를 최소화합니다.
     */
    public void refreshCache() {
        log.info("📦 [PD 산출] 등급 마스터로부터 PD 캐시 갱신 중...");
        gradeMasterRepository.findAll().forEach(grade -> 
            pdCache.put(grade.getRatingCode().toUpperCase(), grade.getPdValue())
        );
        log.info("✅ [PD 산출] 총 {}개의 등급 PD 정보가 캐싱되었습니다.", pdCache.size());
    }

    /**
     * 계좌 및 차주 정보를 기반으로 모델 기준 PD를 산출합니다.
     *
     * 💡 [비즈니스 시나리오]
     * A 고객이 평소 1등급(PD: 0.05%)이라도, 현재 40일째 연체 중이라면 이 고객의 PD는 더 이상 0.05%가 아닙니다.
     * 시스템은 이를 감지하여 PD를 대폭 할증(예: 2배)함으로써 은행의 대손충당금 산출 보수성을 높입니다.
     *
     * @param account   산출 대상 계좌 (연체 정보 포함)
     * @param modelParams 모델 파라미터 (PD Floor 정보)
     * @return 보정 및 Floor가 적용된 최종 PD
     */
    public BigDecimal calculatePd(CrAccount account, AllowanceModelParams modelParams) {
        // 💡 [데이터 준비] 계좌의 주인(고객) 정보를 가져옵니다.
        CrCustomer customer = account.getCustomer();
        
        // 1. 등급 결정 (계좌 등급 우선 > 차주 등급)
        // 💡 특정 대출(계좌)에 별도 등급이 있으면 그것을 쓰고, 없으면 고객의 종합 등급을 씁니다.
        String resolvedRating = (account.getInternalRating() != null) 
                ? account.getInternalRating() 
                : customer.getInternalRating();

        // 2. 기초 PD 조회 (캐시 연동)
        // 💡 [최적화] Repository 직접 조회 대신 메모리에 로드된 pdCache를 활용합니다.
        BigDecimal basePd = getBasePdFromCache(resolvedRating, modelParams.getPdFloor());

        // 3. 동적 PD 보정 (Penalty / 할증 로직)
        BigDecimal penalizedPd = basePd;
        
        // 💡 [위험 보정] 연체 30일 이상이거나 시스템에서 '주의/심각' 경보가 뜬 고객은 부도 확률을 2배로 높여 대손충당금을 대비합니다.
        if (account.getDelinquentDays() >= 30 || 
            "WARNING".equals(customer.getWarningLevel()) || 
            "CRITICAL".equals(customer.getWarningLevel())) {
            
            log.info("📢 [PD 동적 할증 적용] 계좌 {} - 연체일수({}) 또는 신용경보 상태", 
                    account.getAccountNo(), account.getDelinquentDays());
            
            // 단순 할증 로직: 기존 PD의 2배 할증하며, 모델 Floor의 10배를 하한선으로 두어 보수적으로 산출합니다.
            penalizedPd = basePd.multiply(new BigDecimal("2.0"))
                    .max(modelParams.getPdFloor().multiply(new BigDecimal("10.0")));
        }

        // 4. 최종 모델 PD Floor 적용
        // 💡 [IFRS 9 모델] PD는 아무리 낮아도 모델이 정한 최소선(PD Floor, 예: 0.03%)보다 낮을 수 없습니다.
        BigDecimal finalPd = penalizedPd.max(modelParams.getPdFloor());
        
        log.debug("📊 [PD 산출 최종] 계좌번호: {}, 적용등급: {}, 최종 PD: {}", 
                account.getAccountNo(), resolvedRating, finalPd);
        return finalPd;
    }

    /**
     * 캐시에서 PD 값을 안전하게 꺼내옵니다. 캐시가 비어있거나 등급이 없는 경우를 핸들링합니다.
     */
    private BigDecimal getBasePdFromCache(String ratingCode, BigDecimal pdFloor) {
        if (ratingCode == null) return pdFloor;
        
        // 캐시가 비어있으면(Lazy 로딩 지양, 배치 전 미리 채워야 함) 경고 후 직접 조회
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


