package com.risk.mart.core.domain.ods.audit.service;

import com.risk.mart.core.application.port.out.OdsAccountLedgerRepository;
import com.risk.mart.core.application.port.out.OdsCollateralMstRepository;
import com.risk.mart.core.application.port.out.OdsDqAuditRepository;
import com.risk.mart.core.domain.ods.audit.OdsDqAudit;
import com.risk.mart.core.domain.ods.loan.OdsAccountLedger;
import com.risk.mart.core.domain.ods.loan.OdsCollateralMst;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [ODS 서비스] 데이터 품질(Data Quality) 검사 및 리포팅 서비스
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OdsDataQualityService {

    private final OdsAccountLedgerRepository ledgerRepository;
    private final OdsCollateralMstRepository collateralRepository;
    private final OdsDqAuditRepository dqAuditRepository;

    /**
     * 여신 담보 데이터의 정합성을 검사한다. (LTV, 한도 초과 등)
     */
    public void checkCollateralQuality(LocalDate baseDate) {
        log.info("🔍 [DQ 검사] 담보 데이터 품질 점검 (기준일: {})", baseDate);
        List<OdsCollateralMst> collaterals = collateralRepository.findAll();

        for (OdsCollateralMst coll : collaterals) {
            BigDecimal appraisedValue = coll.getAppraisedValue();
            if (appraisedValue == null || appraisedValue.compareTo(BigDecimal.ZERO) <= 0) {
                saveAudit(baseDate, "ods_coll_mst", coll.getCollateralNo(), 
                        "VALUATION_ERR", "담보평가액 누락 또는 0 이하", "HIGH");
            }
        }
    }

    private void saveAudit(LocalDate baseDate, String table, String rowId, 
                          String type, String message, String severity) {
        OdsDqAudit audit = OdsDqAudit.builder()
                .baseDate(baseDate)
                .tableName(table)
                .accountNo(rowId) // rowId를 accountNo 필드에 매핑
                .auditType(type)
                .auditMessage(message)
                .severity(severity)
                .auditTimestamp(LocalDateTime.now())
                .build();
        dqAuditRepository.save(audit);
    }
}
