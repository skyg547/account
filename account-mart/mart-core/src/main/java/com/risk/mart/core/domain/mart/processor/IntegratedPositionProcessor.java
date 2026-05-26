package com.risk.mart.core.domain.mart.processor;

import com.risk.common.entity.IntegratedRiskPosition;
import com.risk.common.enums.CrStaging;
import com.risk.common.enums.CurrencyCode;
import com.risk.common.enums.CustomerType;
import com.risk.mart.core.domain.ods.audit.service.OdsDataQualityService;
import com.risk.mart.core.domain.ods.loan.OdsAccountLedger;
import com.risk.mart.core.domain.ods.loan.OdsAccountRate;
import com.risk.mart.core.domain.ods.loan.OdsEarlyWarning;
import com.risk.mart.core.domain.ods.common.OdsCustomerMst;
import com.risk.mart.core.application.port.out.OdsEarlyWarningRepository;
import com.risk.mart.core.application.port.out.OdsAccountRateRepository;
import com.risk.mart.core.application.port.out.OdsCustomerMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * [마트 프로세서] 통합 리스크 포지션 변환 프로세서 (정규화 버전)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegratedPositionProcessor implements ItemProcessor<OdsAccountLedger, IntegratedRiskPosition>, StepExecutionListener {

    private final OdsEarlyWarningRepository earlyWarningRepository;
    private final OdsAccountRateRepository accountRateRepository;
    private final OdsCustomerMstRepository customerMstRepository;
    private final OdsDataQualityService dqService;
    private LocalDate baseDate;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String baseDateStr = stepExecution.getJobParameters().getString("baseDate");
        this.baseDate = LocalDate.parse(baseDateStr);
        log.info("🚀 [CDM] 통합 포지션 변환 공정을 시작합니다. (기준일: {})", baseDate);
    }

    @Override
    public IntegratedRiskPosition process(OdsAccountLedger ledger) {
        if (ledger.getAccountNo() == null || ledger.getCurrency() == null) {
            log.warn("⚠️ [DQ Failure] 계좌 {}의 필수 데이터 누락. 매핑을 건너뜁니다.", ledger.getAccountNo());
            return null;
        }

        // [정규화] 도메인 포트를 통한 POJO 모델 조회
        Optional<OdsCustomerMst> customerInfo = customerMstRepository.findByCustomerCode(ledger.getCustomerCode());
        Optional<OdsAccountRate> rateInfo = accountRateRepository.findById(ledger.getAccountNo());
        Optional<OdsEarlyWarning> ew = earlyWarningRepository.findTopByCustomerCodeAndBaseDateOrderByBaseDateDesc(ledger.getCustomerCode(), baseDate);

        IntegratedRiskPosition.IntegratedRiskPositionBuilder builder = IntegratedRiskPosition.builder()
                .baseDt(baseDate)
                .accNo(ledger.getAccountNo())
                .customerCode(ledger.getCustomerCode())
                .customerName(customerInfo.map(OdsCustomerMst::getCustomerName).orElse("Unknown"))
                .productCode(ledger.getProductCode())
                .currency(CurrencyCode.valueOf(ledger.getCurrency()))
                .currentBalance(ledger.getOutstandingAmount() != null ? ledger.getOutstandingAmount() : BigDecimal.ZERO)
                .outstandingAmount(ledger.getOutstandingAmount() != null ? ledger.getOutstandingAmount() : BigDecimal.ZERO)
                .limitAmount(ledger.getLimitAmount() != null ? ledger.getLimitAmount() : BigDecimal.ZERO)
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

        // 스테이징 판정
        int days = ledger.getDelinquentDays() != null ? ledger.getDelinquentDays() : 0;
        if (days >= 90) builder.staging(CrStaging.STAGE3);
        else if (days >= 30) builder.staging(CrStaging.STAGE2);
        else builder.staging(CrStaging.STAGE1);

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

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("✅ [CDM] 통합 포지션 변환 완료. (Status: {})", stepExecution.getStatus());
        return null;
    }
}
