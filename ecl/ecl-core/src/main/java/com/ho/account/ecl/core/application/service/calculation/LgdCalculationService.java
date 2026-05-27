package com.ho.account.ecl.core.application.service.calculation;

import com.ho.account.ecl.core.application.port.out.CrLgdSegmentMasterRepository;
import com.ho.account.ecl.core.domain.calculator.IrbRegulatoryParams;
import com.ho.account.ecl.core.domain.model.CrLgdSegmentMaster;
import com.ho.account.shared.finance.enums.CustomerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * [Credit Core] LGD(부도시손실률) 산출 전문 서비스
 * 
 * 💡 [초보자를 위한 개념 설명]
 * LGD (Loss Given Default)란 고객이 부도가 났을 때, 은행이 최종적으로 '떼일 돈의 비율'을 말합니다.
 * 
 * 예시: 
 * 1. 1억을 빌려줬는데 담보가 전혀 없다면? 
 *    - 고객이 망하면 한 푼도 못 건질 수 있으므로 LGD는 높습니다(예: 45%).
 * 2. 1억을 빌려줬는데 시세 2억짜리 아파트를 담보로 잡았다면?
 *    - 고객이 망해도 아파트를 팔아서 돈을 회수할 수 있으므로 LGD는 낮습니다(예: 10%).
 * 
 * 규제적 관점:
 * - 담보가 있는 경우(Secured)와 없는 경우(Unsecured)를 엄격히 구분하여 각각 최소 손실률(Floor)을 보수적으로 적용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LgdCalculationService {

    /** 💡 [초보자 가이드] 차주 유형과 담보 종류에 따른 표준 손실률 규격이 저장된 마스터 저장소입니다. */
    private final CrLgdSegmentMasterRepository lgdSegmentMasterRepository;

    /**
     * 차주 유형 및 담보 유형을 기반으로 규제 준수 LGD를 산출합니다.
     *
     * 💡 [비즈니스 시나리오]
     * 부동산 담보 대출은 차주가 망하더라도 집을 경매에 넘겨 돈을 회수할 수 있습니다.
     * 따라서 무담보 신용대출(기본 LGD 45%)보다 훨씬 낮은 LGD(예: 부동산 담보부 LGD 20%)를 적용받아
     * 은행의 자본 부담을 경감시켜 줍니다.
     *
     * @param customerType   차주 유형 (예: CORPORATE - 기업, RETAIL - 가계)
     * @param collateralType 주담보 유형 (예: REAL_ESTATE - 부동산, FINANCIAL - 금융담보)
     * @param hasCollateral  실제 담보 가액 존재 여부 (가액이 0원 이상인 경우 true)
     * @param irbParams      규제 파라미터 (담보부/무담보부 LGD Floor 정보)
     * @return 세그먼트 매핑 및 Floor가 적용된 최종 LGD (0.0~1.0 사이 값)
     */
    public BigDecimal calculateLgd(String customerType, String collateralType, boolean hasCollateral, IrbRegulatoryParams irbParams) {
        
        // 1. 담보 존재 여부에 따른 유형 확정
        // 💡 담보 가액이 0원이라면 물리적으로 담보가 있더라도 리스크 관점에서는 '무담보(UNSECURED)'로 간주합니다.
        final String resolvedCollateralType = hasCollateral ? collateralType : "UNSECURED";
        
        // 💡 [기본값] 만약 마스터 매핑에 실패할 경우를 대비해 보수적인 무담보 Floor를 기본값으로 설정합니다.
        final BigDecimal defaultLgd = irbParams.getUnsecuredLgdFloor();

        // 2. LGD 세그먼트 마스터 조회
        // 💡 차주의 유형(기업/가계)과 담보의 종류를 조합하여 "정답지(마스터 테이블)"에서 미리 정해진 LGD 수치를 찾아옵니다.
        BigDecimal lgd = lgdSegmentMasterRepository.findByCustomerTypeAndCollateralType(
                        customerType, resolvedCollateralType)
                .map(CrLgdSegmentMaster::getLgdValue)
                .orElseGet(() -> {
                    log.warn("⚠️ [LGD 산출] 세그먼트(차주:{}, 담보:{})를 찾을 수 없습니다. 무담보 규제 최저선({}) 적용",
                            customerType, resolvedCollateralType, defaultLgd);
                    return defaultLgd;
                });

        // 3. 최종 규제 LGD Floor 적용 (담보부/무담보부 분기)
        // 💡 [국제 금융 규제 가이드] 
        //    담보가 있는 자산(Secured)은 LGD가 낮지만, 예상치 못한 가치 하락에 대비해 최소한의 손실률(Secured Floor, 약 10% 등)을 적용합니다. 
        //    무담보(Unsecured)는 훨씬 더 높은 최저선(약 25~45%)을 적용합니다.
        BigDecimal finalLgd = hasCollateral 
                ? lgd.max(irbParams.getSecuredLgdFloor()) 
                : lgd.max(irbParams.getUnsecuredLgdFloor());

        log.debug("📊 [LGD 산출 최종] 차주군: {}, 담보군: {}, 최종 LGD: {}", 
                customerType, resolvedCollateralType, finalLgd);
        return finalLgd;
    }
}
