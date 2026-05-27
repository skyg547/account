package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.ods.common.OdsGeneralLedger;
import java.time.LocalDate;
import java.util.List;

/**
 * [Outbound Port] 총계정원장 데이터 접근 인터페이스
 */
public interface OdsGeneralLedgerRepository {
    List<OdsGeneralLedger> findByBaseDate(java.time.LocalDate baseDate);
    List<OdsGeneralLedger> findAll();
    OdsGeneralLedger save(OdsGeneralLedger gl);
    void saveAll(List<OdsGeneralLedger> gls);
    List<SubjectCurrencyBalanceSummary> getBalanceSummaryByBaseDate(java.time.LocalDate baseDate);

    interface SubjectCurrencyBalanceSummary {
        String getSubjectCode();
        String getCurrencyCode();
        java.math.BigDecimal getBalanceAmount();
    }
}
