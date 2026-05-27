package com.ho.account.ecl.core.domain.result;




import com.ho.account.shared.finance.enums.CrStaging;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulatoryReportSummary {
    private LocalDate baseDate;
    private BigDecimal totalEad;
    private BigDecimal totalRwaSa;
    private BigDecimal totalRwaIrb;
    private BigDecimal capitalSavings;
    private BigDecimal bisRatioImprovement;
    private BigDecimal totalEcl;
    
    private Map<CrStaging, Long> stagingSummary;
    private Map<String, BigDecimal> sectorExposure;
    
    private LocalDate generatedAt;
    private String status;

    public static RegulatoryReportSummary empty(LocalDate baseDate) {
        return RegulatoryReportSummary.builder()
                .baseDate(baseDate)
                .totalEad(BigDecimal.ZERO)
                .totalRwaSa(BigDecimal.ZERO)
                .totalRwaIrb(BigDecimal.ZERO)
                .capitalSavings(BigDecimal.ZERO)
                .bisRatioImprovement(BigDecimal.ZERO)
                .totalEcl(BigDecimal.ZERO)
                .stagingSummary(new HashMap<>())
                .sectorExposure(new HashMap<>())
                .generatedAt(LocalDate.now())
                .status("EMPTY")
                .build();
    }
}
