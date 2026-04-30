package com.ho.account.risk.application.service;

import com.ho.account.loan.domain.Loan;
import com.ho.account.risk.application.port.in.RiskAssessmentUseCase;
import com.ho.account.risk.application.port.out.RiskLoanQueryPort;
import com.ho.account.risk.application.port.out.RiskPersistencePort;
import com.ho.account.risk.domain.CreditRiskExposure;
import com.ho.account.risk.domain.RiskWeightedAsset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * [RiskAssessmentService]
 * 리스크 산출을 위한 핵심 서비스 클래스.
 * 대출 데이터를 기반으로 익스포저를 생성하고 RWA를 계산합니다.
 */
@Service
public class RiskAssessmentService implements RiskAssessmentUseCase {

    private final RiskLoanQueryPort loanQueryPort;
    private final RiskPersistencePort riskPersistencePort;

    public RiskAssessmentService(RiskLoanQueryPort loanQueryPort, RiskPersistencePort riskPersistencePort) {
        this.loanQueryPort = loanQueryPort;
        this.riskPersistencePort = riskPersistencePort;
    }

    @Override
    @Transactional
    public void generateExposures(LocalDate baseDate) {
        // 1. 활성 대출 정보 조회
        List<Loan> activeLoans = loanQueryPort.findAllActiveLoans();

        // 2. 대출 데이터를 기반으로 CreditRiskExposure 생성
        for (Loan loan : activeLoans) {
            CreditRiskExposure exposure = riskPersistencePort.findExposureBySource("LOAN", loan.getLoanNumber(), baseDate)
                    .orElse(new CreditRiskExposure());

            exposure.setBaseDate(baseDate);
            exposure.setSourceSystemId("LOAN");
            exposure.setSourceReferenceId(loan.getLoanNumber());
            exposure.setBusinessPartner(loan.getBusinessPartner());
            exposure.setEadAmount(loan.getPrincipalAmount()); // EAD 기본값: 원금 잔액
            
            // 리스크 파라미터 연동
            String assetClass = "CORPORATE"; // TODO: 차주/상품 마스터 정보에서 가져오는 로직 필요
            exposure.setAssetClass(assetClass);
            
            BigDecimal pdRate = riskPersistencePort.findParameterValue("PD_GUIDE", assetClass, "SA", baseDate)
                    .orElse(new BigDecimal("0.03")); // 기본값 3%
            BigDecimal lgdRate = riskPersistencePort.findParameterValue("LGD_GUIDE", assetClass, "SA", baseDate)
                    .orElse(new BigDecimal("0.45")); // 기본값 45%
            
            exposure.setPdRate(pdRate);
            exposure.setLgdRate(lgdRate);

            exposure.calculateExpectedLoss();
            riskPersistencePort.saveExposure(exposure);
        }
    }

    @Override
    @Transactional
    public void calculateRwa(LocalDate baseDate, String approachType) {
        // 1. 해당 일자의 익스포저 조회
        List<CreditRiskExposure> exposures = riskPersistencePort.findExposuresByBaseDate(baseDate);

        // 2. RWA 계산 (RiskParameter 연동)
        for (CreditRiskExposure exposure : exposures) {
            RiskWeightedAsset rwa = new RiskWeightedAsset();
            rwa.setExposure(exposure);
            rwa.setCalculationDate(LocalDate.now());
            rwa.setApproachType(approachType);

            // 위험가중치(RW) 조회
            BigDecimal riskWeight = riskPersistencePort.findParameterValue("RW", exposure.getAssetClass(), approachType, baseDate)
                    .orElse(new BigDecimal("1.00")); // 기본값 100%
            
            rwa.setRiskWeight(riskWeight);

            rwa.calculateRwa();
            riskPersistencePort.saveRwa(rwa);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskWeightedAsset> getRwaResults(LocalDate startDate, LocalDate endDate) {
        return riskPersistencePort.findRwaResultsBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditRiskExposure> getExposuresByBusinessPartner(String businessPartnerCode) {
        // TODO: 필요한 경우 PersistencePort에 메서드 추가 구현
        return List.of();
    }
}
