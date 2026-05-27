package com.ho.account.mart.core.application.port.out;

import com.ho.account.shared.finance.entity.IntegratedRiskPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * [Port] 통합 리스크 포지션 데이터 마트 데이터소스 인터페이스.
 */
public interface IntegratedRiskPositionRepository {

    void deleteByBaseDt(LocalDate baseDt);

    Page<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt, Pageable pageable);

    List<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt);

    List<ProductCurrencyBalanceSummary> getBalanceSummaryByBaseDate(LocalDate baseDt);

    List<StagingDistribution> getStagingDistribution(LocalDate baseDt);

    List<SectorDistribution> getSectorDistribution(LocalDate baseDt);

    Map<String, Object> getSummaryMetrics(LocalDate baseDt);

    List<RatingDistribution> getRatingDistribution(LocalDate baseDt);

    IntegratedRiskPosition save(IntegratedRiskPosition position);

    void saveAll(Iterable<IntegratedRiskPosition> positions);

    interface StagingDistribution {
        String getStaging();
        BigDecimal getValue();
    }

    interface SectorDistribution {
        String getSector();
        BigDecimal getValue();
    }

    interface RatingDistribution {
        String getRating();
        BigDecimal getRwa();
        BigDecimal getExposure();
    }

    interface ProductCurrencyBalanceSummary {
        String getProductCode();
        String getCurrencyCode();
        BigDecimal getBalanceAmount();
    }
}
