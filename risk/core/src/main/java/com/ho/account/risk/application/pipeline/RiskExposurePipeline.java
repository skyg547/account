package com.ho.account.risk.application.pipeline;

import com.ho.account.loan.domain.Loan;
import com.ho.account.risk.application.port.out.RiskPersistencePort;
import com.ho.account.risk.domain.CreditRiskExposure;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * [RiskExposurePipeline]
 * 대량의 대출 데이터를 리스크 익스포저 데이터로 변환하는 배치 전용 파이프라인.
 * Chunk 단위 처리 중 비즈니스 규칙 변환을 담당합니다.
 */
@Component
public class RiskExposurePipeline {

    private final RiskPersistencePort riskPersistencePort;

    public RiskExposurePipeline(RiskPersistencePort riskPersistencePort) {
        this.riskPersistencePort = riskPersistencePort;
    }

    /**
     * 개별 대출 객체를 익스포저 엔티티로 변환합니다.
     */
    public CreditRiskExposure process(Loan loan, LocalDate baseDate) {
        CreditRiskExposure exposure = new CreditRiskExposure();
        exposure.setBaseDate(baseDate);
        exposure.setSourceSystemId("LOAN");
        exposure.setSourceReferenceId(loan.getLoanNumber());
        exposure.setBusinessPartner(loan.getBusinessPartner());
        exposure.setEadAmount(loan.getPrincipalAmount());
        
        // 자산 클래스 결정 로직 (Batch 전용 고속 매핑)
        String assetClass = determineAssetClass(loan);
        exposure.setAssetClass(assetClass);
        
        // 리스크 파라미터 적용 (Cache 적용 권장)
        BigDecimal pdRate = riskPersistencePort.findParameterValue("PD_GUIDE", assetClass, "SA", baseDate)
                .orElse(new BigDecimal("0.03"));
        BigDecimal lgdRate = riskPersistencePort.findParameterValue("LGD_GUIDE", assetClass, "SA", baseDate)
                .orElse(new BigDecimal("0.45"));
        
        exposure.setPdRate(pdRate);
        exposure.setLgdRate(lgdRate);
        exposure.calculateExpectedLoss();
        
        return exposure;
    }

    private String determineAssetClass(Loan loan) {
        // 상품 유형에 따른 자산 분류 (예시)
        if (loan.getLoanType() == Loan.LoanType.MORTGAGE) {
            return "RESIDENTIAL_MORTGAGE";
        }
        return "CORPORATE";
    }
}
