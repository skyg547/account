package com.ho.account.mart.core.domain.mart.processor;

import com.ho.account.mart.core.application.port.out.ExchangeRateRepository;
import com.ho.account.mart.core.application.port.out.OdsAccountRateRepository;
import com.ho.account.mart.core.application.port.out.OdsCustomerMstRepository;
import com.ho.account.mart.core.application.port.out.OdsEarlyWarningRepository;
import com.ho.account.mart.core.domain.marketdata.ExchangeRate;
import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import com.ho.account.mart.core.domain.ods.common.OdsCustomerMst;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountRate;
import com.ho.account.mart.core.domain.ods.loan.OdsEarlyWarning;
import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.shared.finance.enums.CustomerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * [마트 프로세서] 대손충당금 입력 포지션 변환 규칙.
 *
 * <p>초보자 설명: 이 클래스는 ODS 계좌 원장을 IFRS 9 ECL 산출 입력값으로 바꾸는
 * 업무 규칙을 담는다. Spring Batch가 한 건씩 호출하는 기술 흐름은 mart-batch adapter가 담당하고,
 * 이 core 클래스는 기준일과 원장 데이터를 받아 순수하게 업무 데이터를 만든다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegratedPositionProcessor {

    private final OdsEarlyWarningRepository earlyWarningRepository;
    private final OdsAccountRateRepository accountRateRepository;
    private final OdsCustomerMstRepository customerMstRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public AllowanceInputPosition process(OdsAccountLedger ledger, LocalDate baseDate) {
        if (ledger.getAccountNo() == null || ledger.getCurrency() == null) {
            log.warn("⚠️ [DQ Failure] 계좌 {}의 필수 데이터 누락. 매핑을 건너뜁니다.", ledger.getAccountNo());
            return null;
        }

        // [정규화] 도메인 포트를 통한 POJO 모델 조회
        Optional<OdsCustomerMst> customerInfo = customerMstRepository.findByCustomerCode(ledger.getCustomerCode());
        Optional<OdsAccountRate> rateInfo = accountRateRepository.findById(ledger.getAccountNo());
        Optional<OdsEarlyWarning> ew = earlyWarningRepository.findTopByCustomerCodeAndBaseDateOrderByBaseDateDesc(ledger.getCustomerCode(), baseDate);
        BigDecimal outstandingAmount = ledger.getOutstandingAmount() != null ? ledger.getOutstandingAmount() : BigDecimal.ZERO;
        BigDecimal marketValue = convertToKrw(ledger.getCurrency(), outstandingAmount, baseDate);

        AllowanceInputPosition.AllowanceInputPositionBuilder builder = AllowanceInputPosition.builder()
                .baseDt(baseDate)
                .accNo(ledger.getAccountNo())
                .customerCode(ledger.getCustomerCode())
                .customerName(customerInfo.map(OdsCustomerMst::getCustomerName).orElse("Unknown"))
                .productCode(ledger.getProductCode())
                .currency(CurrencyCode.valueOf(ledger.getCurrency()))
                .currentBalance(outstandingAmount)
                .outstandingAmount(outstandingAmount)
                .limitAmount(ledger.getLimitAmount() != null ? ledger.getLimitAmount() : BigDecimal.ZERO)
                .marketValue(marketValue)
                .interestRate(ledger.getInterestRate())
                .spread(ledger.getSpread())
                .baseRateCode(ledger.getBaseRateCode())
                .rateType(ledger.getInterestRate() != null ? "FIXED" : "FLOATING")
                .nextResetDate(ledger.getNextResetDate())
                .openDate(ledger.getOpenDate())
                .maturityDate(ledger.getMaturityDate())
                .delinquentDays(ledger.getDelinquentDays() != null ? ledger.getDelinquentDays() : 0)
                .repaymentMethod(ledger.getRepaymentMethod())
                .gracePeriod(ledger.getGracePeriod())
                .repaymentFreq(ledger.getRepaymentFreq())
                .branchCd(ledger.getBranchCd())
                .bizUnitCd(ledger.getBizUnitCd())
                .warningLevel(ew.map(OdsEarlyWarning::getWarningLevel).orElse("NORMAL"))
                .isDebtRestructured(false);

        builder.staging(ledger.determineStaging());

        // [정규화] 고객 정보 매핑 및 Enum 타입 변환
        customerInfo.ifPresent(ci -> {
            try {
                builder.customerType(CustomerType.valueOf(ci.getCustomerType()));
            } catch (Exception e) {
                builder.customerType(CustomerType.RETAIL); // 기본값
            }
            builder.isSme(ci.getIsSme());
            builder.internalRating(ci.getInternalRating());
            builder.industryCode(ci.getIndustryCode());
            builder.countryCode(ci.getCountryCode());
        });

        // 금리 상/하한 매핑
        rateInfo.ifPresent(ri -> {
            builder.interestRateCap(ri.getInterestRateCap());
            builder.interestRateFloor(ri.getInterestRateFloor());
            builder.refIndexCode(ri.getRefIndexCode());
        });

        builder.marginRate(BigDecimal.ZERO);
        builder.repricingFreq(0);

        return builder.build();
    }

    private BigDecimal convertToKrw(String currency, BigDecimal amount, LocalDate baseDate) {
        if (currency == null || CurrencyCode.KRW.name().equals(currency) || baseDate == null) {
            return amount.setScale(4, RoundingMode.HALF_UP);
        }

        CurrencyCode baseCurrency = CurrencyCode.valueOf(currency);
        return exchangeRateRepository.findByBaseDateAndBaseCurrencyAndQuoteCurrency(
                        baseDate,
                        baseCurrency,
                        CurrencyCode.KRW)
                .map(ExchangeRate::getBaseRate)
                .map(rate -> amount.multiply(rate).setScale(4, RoundingMode.HALF_UP))
                .orElseGet(() -> amount.setScale(4, RoundingMode.HALF_UP));
    }
}
