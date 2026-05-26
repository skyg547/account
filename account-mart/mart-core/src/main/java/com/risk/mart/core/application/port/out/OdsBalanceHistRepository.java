package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.ods.common.OdsBalanceHist;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 계좌별 잔액 이력 데이터 접근 인터페이스
 */
public interface OdsBalanceHistRepository {
    Optional<OdsBalanceHist> findTopByAccountNoOrderByBaseDateDesc(String accountNo);
    List<OdsBalanceHist> findAll();
    OdsBalanceHist save(OdsBalanceHist balanceHist);
    void saveAll(List<OdsBalanceHist> balanceHists);
    List<BalanceSummary> findBalanceSummaryByBaseDate(java.time.LocalDate baseDate);

    interface BalanceSummary {
        String getSubjectCode();
        String getCurrencyCode();
        java.math.BigDecimal getBalanceAmount();
    }
}
