package com.ho.account.mart.core.domain.ods.audit.service;

import com.ho.account.mart.core.application.port.out.OdsCollateralMstRepository;
import com.ho.account.mart.core.application.port.out.OdsDqAuditRepository;
import com.ho.account.mart.core.application.service.ods.CollateralDataQualityInspectionService;
import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * [ODS 서비스] 데이터 품질(Data Quality) 검사 및 리포팅 서비스
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OdsDataQualityService {

    private final OdsCollateralMstRepository collateralRepository;
    private final OdsDqAuditRepository dqAuditRepository;
    private final CollateralDataQualityInspectionService collateralInspectionService;

    /**
     * 여신 담보 데이터의 정합성을 검사한다. (LTV, LGD 선행 상세, 한도 초과 등)
     *
     * <p>초보자 설명: 여기서는 전체 담보 목록을 가져와 같은 core DQ 유즈케이스를 반복 호출합니다.
     * Batch Step도 동일한 유즈케이스를 사용하므로 화면/API 호출과 배치 실행의 판단 기준이 갈라지지 않습니다.</p>
     */
    public void checkCollateralQuality(LocalDate baseDate) {
        log.info("🔍 [DQ 검사] 담보 데이터 품질 점검 (기준일: {})", baseDate);
        List<OdsCollateralMst> collaterals = collateralRepository.findAll();

        for (OdsCollateralMst collateral : collaterals) {
            OdsDqAudit audit = collateralInspectionService.inspect(collateral, baseDate);
            if (audit != null) {
                dqAuditRepository.save(audit);
            }
        }
    }
}
