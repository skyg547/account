package com.ho.account.mart.core.domain.ods.audit.processor;

import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.loan.OdsApartCollDetail;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * [DQ 규칙] 담보 데이터 품질 검증 규칙.
 * 💡 [비즈니스 의미] 담보 가액이나 헤어컷 비율 등을 검증하여
 *    LGD/ECL 산출 시 담보 인정 가액이 누락되는 것을 방지합니다.
 *
 * <p>초보자 설명: 이 클래스는 DB를 직접 조회하지 않고 "이미 전달받은 담보 마스터/상세 값"만 보고
 * 오류 여부를 판단합니다. 상세 데이터 조회는 application service가 port를 통해 수행합니다.</p>
 */
@Component
public class CollateralDataQualityProcessor {

    @Nullable
    public OdsDqAudit inspect(@NonNull OdsCollateralMst coll, @NonNull LocalDate baseDate) {
        return inspect(coll, Optional.empty(), baseDate);
    }

    @Nullable
    public OdsDqAudit inspect(@NonNull OdsCollateralMst coll,
                              @NonNull Optional<OdsApartCollDetail> apartmentDetail,
                              @NonNull LocalDate baseDate) {
        BigDecimal recognizedAmount = coll.getAppraisedValue();

        if (!isPositive(recognizedAmount)) {
            return audit(baseDate,
                    "ods_coll_mst",
                    coll.getCollateralNo(),
                    "LOGIC_ERROR",
                    "담보가 존재하나 인정 가액이 0이거나 누락됨 (LTV 산출 불가)",
                    "WARNING");
        }

        if (!requiresApartmentDetail(coll)) {
            return null;
        }

        if (apartmentDetail.isEmpty()) {
            return audit(baseDate,
                    "ods_apart_coll_detail",
                    coll.getCollateralNo(),
                    "MISSING_REFERENCE",
                    "부동산/아파트 담보이나 상세 시세 데이터가 없어 LGD 선행값 검증 불가",
                    "WARNING");
        }

        OdsApartCollDetail detail = apartmentDetail.get();
        if (!detail.hasRequiredLgdInputs()) {
            List<String> reasons = detail.missingLgdInputReasons();
            return audit(baseDate,
                    "ods_apart_coll_detail",
                    coll.getCollateralNo(),
                    "LGD_INPUT_ERROR",
                    "아파트 담보 상세 LGD 입력값 오류: " + String.join(", ", reasons),
                    "WARNING");
        }

        return null;
    }

    public boolean requiresApartmentDetail(@NonNull OdsCollateralMst coll) {
        String type = normalize(coll.getCollateralType());
        return "REAL_ESTATE".equals(type)
                || "APARTMENT".equals(type)
                || "APART".equals(type)
                || "APT".equals(type);
    }

    private OdsDqAudit audit(LocalDate baseDate,
                             String tableName,
                             String rowId,
                             String auditType,
                             String message,
                             String severity) {
        return OdsDqAudit.builder()
                .baseDate(baseDate)
                .tableName(tableName)
                .accountNo(rowId)
                .auditType(auditType)
                .auditMessage(message)
                .severity(severity)
                .auditTimestamp(LocalDateTime.now())
                .build();
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
