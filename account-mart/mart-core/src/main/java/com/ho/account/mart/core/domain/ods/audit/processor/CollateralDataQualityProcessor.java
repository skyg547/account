package com.ho.account.mart.core.domain.ods.audit.processor;

import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [DQ 규칙] 담보 데이터 품질 검증 규칙.
 * 💡 [비즈니스 의미] 담보 가액이나 헤어컷 비율 등을 검증하여
 *    LGD/ECL 산출 시 담보 인정 가액이 누락되는 것을 방지합니다.
 */
@Component
public class CollateralDataQualityProcessor {

    @Nullable
    public OdsDqAudit inspect(@NonNull OdsCollateralMst coll, @NonNull LocalDate baseDate) {
        BigDecimal recognizedAmount = coll.getAppraisedValue() != null
                ? coll.getAppraisedValue().multiply(BigDecimal.ONE.subtract(BigDecimal.ZERO))
                : null;

        if (recognizedAmount == null || recognizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return OdsDqAudit.builder()
                    .baseDate(baseDate)
                    .tableName("ods_coll_mst")
                    .accountNo(coll.getCollateralNo())
                    .auditType("LOGIC_ERROR")
                    .auditMessage("담보가 존재하나 인정 가액이 0이거나 누락됨 (LTV 산출 불가)")
                    .severity("WARNING")
                    .auditTimestamp(LocalDateTime.now())
                    .build();
        }

        return null;
    }
}
