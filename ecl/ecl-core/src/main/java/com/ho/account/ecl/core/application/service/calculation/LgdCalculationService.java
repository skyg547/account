package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrLgdSegmentMasterRepository;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.calculator.LgdCalculator;
import com.ho.account.ecl.core.domain.model.CrLgdSegmentMaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * [애플리케이션 서비스] LGD(부도시손실률) 산출 조율 서비스 (LGD Calculation Application Service)
 * 
 * 💡 [DDD 설계 원칙]
 * 본 서비스는 차주 및 담보 세그먼트에 따른 표준 LGD 마스터 수치를 조회(Orchestration)하고,
 * 담보부/무담보부 LGD Floor 규제 반영은 Pure Domain Calculator인 {@link LgdCalculator}로 위임합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LgdCalculationService {

    /** 차주 유형과 담보 종류별 LGD 표준 데이터 저장소 포트 */
    private final CrLgdSegmentMasterRepository lgdSegmentMasterRepository;
    
    /** Pure Domain Calculator */
    private final LgdCalculator lgdCalculator;

    /**
     * 차주 유형 및 담보 유형을 기반으로 LGD 세그먼트를 조회하고 Domain Calculator를 통해 최종 LGD를 산출합니다.
     *
     * @param customerType   차주 유형
     * @param collateralType 주담보 유형
     * @param hasCollateral  실제 담보 가액 존재 여부
     * @param modelParams    모델 파라미터
     * @return Floor 규제가 적용된 최종 LGD
     */
    public BigDecimal calculateLgd(String customerType, String collateralType, boolean hasCollateral, AllowanceModelParams modelParams) {
        final String resolvedCollateralType = hasCollateral ? collateralType : "UNSECURED";
        final BigDecimal defaultLgd = modelParams.getUnsecuredLgdFloor();

        // 1. LGD 세그먼트 마스터 조회 (Infrastructure IO)
        BigDecimal baseLgd = lgdSegmentMasterRepository.findByCustomerTypeAndCollateralType(
                        customerType, resolvedCollateralType)
                .map(CrLgdSegmentMaster::getLgdValue)
                .orElseGet(() -> {
                    log.warn("⚠️ [LGD 산출] 세그먼트(차주:{}, 담보:{})를 찾을 수 없습니다. 무담보 모델 최저선({}) 적용",
                            customerType, resolvedCollateralType, defaultLgd);
                    return defaultLgd;
                });

        // 2. Pure Domain Calculator 위임 호출 (Floor 적용)
        BigDecimal finalLgd = lgdCalculator.applyLgdFloor(
                baseLgd,
                hasCollateral,
                modelParams.getSecuredLgdFloor(),
                modelParams.getUnsecuredLgdFloor()
        );

        log.debug("📊 [LGD 산출 최종] 차주군: {}, 담보군: {}, 최종 LGD: {}", 
                customerType, resolvedCollateralType, finalLgd);
        return finalLgd;
    }
}



