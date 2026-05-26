package com.risk.credit.core.application.service.monitoring;

import com.risk.credit.core.application.port.out.IntegratedRiskPositionRepository;
import com.risk.credit.core.domain.result.RegulatoryReportSummary;


import com.risk.common.enums.CrStaging;
import com.risk.common.entity.IntegratedRiskPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * �� [Reporting] Regulatory Report Service.
 * Aggregates multi-dimensional risk metrics for official disclosure and
 * management summary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegulatoryReportService {

        private final IntegratedRiskPositionRepository martRepository;

        @Transactional(readOnly = true)
        public RegulatoryReportSummary generateExecutiveSummary(LocalDate baseDate) {
                log.info("Generating Regulatory Executive Summary for baseDate: {}", baseDate);

                List<IntegratedRiskPosition> positions = martRepository.findByBaseDt(baseDate);

                if (positions.isEmpty()) {
                        return RegulatoryReportSummary.empty(baseDate);
                }

                // 1. Basic Aggregations
                BigDecimal totalEad = positions.stream()
                                .map(p -> p.getOutstandingAmount() != null ? p.getOutstandingAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalRwaSa = positions.stream()
                                .map(p -> p.getRwaSa() != null ? p.getRwaSa() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalRwaIrb = positions.stream()
                                .map(p -> p.getRwaIrb() != null ? p.getRwaIrb() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalEcl = positions.stream()
                                .map(p -> p.getExpectedLoss() != null ? p.getExpectedLoss() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 2. RWA Savings Analysis
                BigDecimal capitalSavings = totalRwaSa.subtract(totalRwaIrb);
                BigDecimal bisRatioImprovement = BigDecimal.ZERO;
                if (totalEad.compareTo(BigDecimal.ZERO) > 0) {
                        // Pseudo-BIS impact (assuming 8% capital charge)
                        bisRatioImprovement = capitalSavings.multiply(new BigDecimal("0.08"))
                                        .divide(totalEad, 4, RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100"));
                }

                // 3. Staging Distribution
                Map<CrStaging, Long> stagingCount = positions.stream()
                                .collect(Collectors.groupingBy(
                                                p -> p.getStaging() != null ? p.getStaging() : CrStaging.STAGE1,
                                                Collectors.counting()));

                // 4. Sector Concentration (HHI Logic)
                Map<String, BigDecimal> sectorExposure = positions.stream()
                                .collect(Collectors.groupingBy(
                                                p -> p.getIndustryCode() != null ? p.getIndustryCode() : "N/A",
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                p -> p.getOutstandingAmount() != null
                                                                                ? p.getOutstandingAmount()
                                                                                : BigDecimal.ZERO,
                                                                BigDecimal::add)));

                return RegulatoryReportSummary.builder()
                                .baseDate(baseDate)
                                .totalEad(totalEad)
                                .totalRwaSa(totalRwaSa)
                                .totalRwaIrb(totalRwaIrb)
                                .capitalSavings(capitalSavings)
                                .bisRatioImprovement(bisRatioImprovement)
                                .totalEcl(totalEcl)
                                .stagingSummary(stagingCount)
                                .sectorExposure(sectorExposure)
                                .generatedAt(LocalDate.now())
                                .status("FINAL")
                                .build();
        }
}
