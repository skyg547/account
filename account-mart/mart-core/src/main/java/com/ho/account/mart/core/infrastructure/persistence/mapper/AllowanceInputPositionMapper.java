package com.ho.account.mart.core.infrastructure.persistence.mapper;

import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import com.ho.account.mart.core.infrastructure.persistence.entity.mart.AllowanceInputPositionEntity;

/**
 * [Mapper] IFRS 9 대손충당금 입력 포지션 Domain POJO <-> JPA Entity 데이터 매퍼.
 *
 * <p><strong>교육적 주석 (Pedagogical Comments):</strong></p>
 * 헥사고날 아키텍처에서는 Domain 모델과 Infrastructure JPA Entity 간의 결합도를 낮추기 위해
 * Data Mapper 패턴을 사용합니다. 도메인 레이어는 DB 영속 기술에 독립성을 유지하며,
 * 매퍼 객체가 양방향 변환을 독립적으로 수행합니다.
 */
public class AllowanceInputPositionMapper {

    private AllowanceInputPositionMapper() {
        // 매퍼 인스턴스화 방지
    }

    public static AllowanceInputPosition toDomain(AllowanceInputPositionEntity entity) {
        if (entity == null) {
            return null;
        }
        return AllowanceInputPosition.builder()
                .baseDt(entity.getBaseDt())
                .accNo(entity.getAccNo())
                .customerCode(entity.getCustomerCode())
                .customerName(entity.getCustomerName())
                .customerType(entity.getCustomerType())
                .isSme(entity.getIsSme())
                .countryCode(entity.getCountryCode())
                .productCode(entity.getProductCode())
                .productCategory(entity.getProductCategory())
                .currency(entity.getCurrency())
                .bsClass(entity.getBsClass())
                .currentBalance(entity.getCurrentBalance())
                .outstandingAmount(entity.getOutstandingAmount())
                .limitAmount(entity.getLimitAmount())
                .marketValue(entity.getMarketValue())
                .interestRate(entity.getInterestRate())
                .couponRate(entity.getCouponRate())
                .rateType(entity.getRateType())
                .baseRateCode(entity.getBaseRateCode())
                .spread(entity.getSpread())
                .paymentFreq(entity.getPaymentFreq())
                .nextResetDate(entity.getNextResetDate())
                .openDate(entity.getOpenDate())
                .maturityDate(entity.getMaturityDate())
                .repaymentMethod(entity.getRepaymentMethod())
                .gracePeriod(entity.getGracePeriod())
                .repaymentFreq(entity.getRepaymentFreq())
                .interestRateFloor(entity.getInterestRateFloor())
                .interestRateCap(entity.getInterestRateCap())
                .refIndexCode(entity.getRefIndexCode())
                .internalRating(entity.getInternalRating())
                .externalRating(entity.getExternalRating())
                .industryCode(entity.getIndustryCode())
                .warningLevel(entity.getWarningLevel())
                .isDebtRestructured(entity.getIsDebtRestructured())
                .delinquentDays(entity.getDelinquentDays())
                .staging(entity.getStaging())
                .collateralAmount(entity.getCollateralAmount())
                .recognizedCollateralAmount(entity.getRecognizedCollateralAmount())
                .collateralType(entity.getCollateralType())
                .pd(entity.getPd())
                .lgd(entity.getLgd())
                .expectedLoss(entity.getExpectedLoss())
                .branchCd(entity.getBranchCd())
                .bizUnitCd(entity.getBizUnitCd())
                .marginRate(entity.getMarginRate())
                .repricingFreq(entity.getRepricingFreq())
                .build();
    }

    public static AllowanceInputPositionEntity toEntity(AllowanceInputPosition domain) {
        if (domain == null) {
            return null;
        }
        return AllowanceInputPositionEntity.builder()
                .baseDt(domain.getBaseDt())
                .accNo(domain.getAccNo())
                .customerCode(domain.getCustomerCode())
                .customerName(domain.getCustomerName())
                .customerType(domain.getCustomerType())
                .isSme(domain.getIsSme())
                .countryCode(domain.getCountryCode())
                .productCode(domain.getProductCode())
                .productCategory(domain.getProductCategory())
                .currency(domain.getCurrency())
                .bsClass(domain.getBsClass())
                .currentBalance(domain.getCurrentBalance())
                .outstandingAmount(domain.getOutstandingAmount())
                .limitAmount(domain.getLimitAmount())
                .marketValue(domain.getMarketValue())
                .interestRate(domain.getInterestRate())
                .couponRate(domain.getCouponRate())
                .rateType(domain.getRateType())
                .baseRateCode(domain.getBaseRateCode())
                .spread(domain.getSpread())
                .paymentFreq(domain.getPaymentFreq())
                .nextResetDate(domain.getNextResetDate())
                .openDate(domain.getOpenDate())
                .maturityDate(domain.getMaturityDate())
                .repaymentMethod(domain.getRepaymentMethod())
                .gracePeriod(domain.getGracePeriod())
                .repaymentFreq(domain.getRepaymentFreq())
                .interestRateFloor(domain.getInterestRateFloor())
                .interestRateCap(domain.getInterestRateCap())
                .refIndexCode(domain.getRefIndexCode())
                .internalRating(domain.getInternalRating())
                .externalRating(domain.getExternalRating())
                .industryCode(domain.getIndustryCode())
                .warningLevel(domain.getWarningLevel())
                .isDebtRestructured(domain.getIsDebtRestructured())
                .delinquentDays(domain.getDelinquentDays())
                .staging(domain.getStaging())
                .collateralAmount(domain.getCollateralAmount())
                .recognizedCollateralAmount(domain.getRecognizedCollateralAmount())
                .collateralType(domain.getCollateralType())
                .pd(domain.getPd())
                .lgd(domain.getLgd())
                .expectedLoss(domain.getExpectedLoss())
                .branchCd(domain.getBranchCd())
                .bizUnitCd(domain.getBizUnitCd())
                .marginRate(domain.getMarginRate())
                .repricingFreq(domain.getRepricingFreq())
                .build();
    }
}
