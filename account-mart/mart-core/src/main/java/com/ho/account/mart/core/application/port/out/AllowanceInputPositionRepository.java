package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * [Port] 대손충당금 입력 포지션 데이터소스 인터페이스.
 */
public interface AllowanceInputPositionRepository {

    void deleteByBaseDt(LocalDate baseDt);

    Page<AllowanceInputPosition> findByBaseDt(LocalDate baseDt, Pageable pageable);

    List<AllowanceInputPosition> findByBaseDt(LocalDate baseDt);

    List<ProductCurrencyBalanceSummary> getBalanceSummaryByBaseDate(LocalDate baseDt);

    List<StagingDistribution> getStagingDistribution(LocalDate baseDt);

    List<SectorDistribution> getSectorDistribution(LocalDate baseDt);

    Map<String, Object> getSummaryMetrics(LocalDate baseDt);

    AllowanceInputPosition save(AllowanceInputPosition position);

    void saveAll(Iterable<AllowanceInputPosition> positions);

    interface StagingDistribution {
        String getStaging();
        BigDecimal getValue();
    }

    interface SectorDistribution {
        String getSector();
        BigDecimal getValue();
    }

    interface ProductCurrencyBalanceSummary {
        String getProductCode();
        String getCurrencyCode();
        BigDecimal getBalanceAmount();
    }
}

