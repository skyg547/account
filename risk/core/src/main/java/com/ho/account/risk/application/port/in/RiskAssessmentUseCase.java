package com.ho.account.risk.application.port.in;

import com.ho.account.risk.domain.CreditRiskExposure;
import com.ho.account.risk.domain.RiskWeightedAsset;
import java.time.LocalDate;
import java.util.List;

/**
 * [RiskAssessmentUseCase]
 * 리스크 산출 및 조회를 위한 인바운드 포트(UseCase).
 */
public interface RiskAssessmentUseCase {

    /**
     * 특정 기준일의 RDM(익스포저) 데이터를 생성/갱신합니다.
     */
    void generateExposures(LocalDate baseDate);

    /**
     * 특정 기준일의 RWA를 산출합니다.
     */
    void calculateRwa(LocalDate baseDate, String approachType);

    /**
     * 특정 기간의 RWA 산출 결과를 조회합니다.
     */
    List<RiskWeightedAsset> getRwaResults(LocalDate startDate, LocalDate endDate);

    /**
     * 특정 차주의 신용 노출 현황을 조회합니다.
     */
    List<CreditRiskExposure> getExposuresByBusinessPartner(String businessPartnerCode);
}
