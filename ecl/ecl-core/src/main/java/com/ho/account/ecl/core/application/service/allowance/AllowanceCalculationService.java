package com.ho.account.ecl.core.application.service.allowance;

import com.ho.account.ecl.core.application.pipeline.AllowanceDataQualityService;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
import com.ho.account.ecl.core.application.service.calculation.CcfCalculationService;
import com.ho.account.ecl.core.application.service.calculation.EadCrmCalculationService;
import com.ho.account.ecl.core.application.service.calculation.ForwardLookingEclService;
import com.ho.account.ecl.core.application.service.calculation.AllowanceParameterService;
import com.ho.account.ecl.core.application.service.calculation.LgdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.LifetimePdService;
import com.ho.account.ecl.core.application.service.calculation.PdCalculationService;
import com.ho.account.ecl.core.application.service.calculation.StagingService;
import com.ho.account.ecl.core.domain.calculator.AllowanceModelParams;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.exposure.CrCustomer;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.shared.finance.enums.CalculationStatus;
import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CustomerType;
import com.ho.account.shared.finance.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IFRS 9 대손충당금 산출 유즈케이스 서비스.
 *
 * 이 서비스는 결산 목적의 Stage, PD, EAD, LGD, 미래전망 ECL 산출만 담당한다.
 * 대손충당금, 회계 summary 지표는 표준 대손충당금 경로에 포함하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllowanceCalculationService {

    private final CrAccountRepository accountRepository;
    private final AllowanceEclResultRepository resultRepository;
    private final AllowanceDataQualityService dataQualityService;
    private final CrBulkOperationPort bulkOperationPort;
    private final StagingService stagingService;
    private final PdCalculationService pdCalculationService;
    private final LgdCalculationService lgdCalculationService;
    private final CcfCalculationService ccfCalculationService;
    private final EadCrmCalculationService eadCrmCalculationService;
    private final LifetimePdService lifetimePdService;
    private final ForwardLookingEclService forwardLookingEclService;
    private final AllowanceParameterService parameterService;

    @Transactional
    public void clearPreviousResults(LocalDate baseDate) {
        bulkOperationPort.clearBatchResults(baseDate);
    }

    @Transactional(readOnly = true)
    public void refreshAllCaches(LocalDate baseDate) {
        log.info("[IFRS9 Allowance] 기준 데이터 캐시 로드 시작. baseDate={}", baseDate);

        stagingService.refreshRankCache();
        pdCalculationService.refreshCache();
        ccfCalculationService.refreshCache();
        parameterService.refreshCache();
        lifetimePdService.refreshCache(baseDate);
        forwardLookingEclService.refreshCache();

        log.info("[IFRS9 Allowance] 기준 데이터 캐시 로드 완료. baseDate={}", baseDate);
    }

    @Transactional
    @NonNull
    public AllowanceEclResult calculateAccountAllowance(@NonNull Long accountId, @NonNull LocalDate baseDate) {
        CrAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CrAccount", accountId));

        if (!dataQualityService.validate(account)) {
            AllowanceEclResult failedResult = AllowanceEclResult.builder()
                    .id(account.getId())
                    .baseDate(baseDate)
                    .account(account)
                    .staging(CrStaging.STAGE1)
                    .status(CalculationStatus.FAILED)
                    .errorMessage("IFRS9 allowance DQ validation failed")
                    .calculationCompletedAt(LocalDateTime.now())
                    .build();
            resultRepository.save(failedResult);
            return failedResult;
        }

        CrCustomer customer = account.getCustomer();
        CustomerType customerType = customer.getCustomerType();
        AllowanceModelParams modelParams = parameterService.getParameters();

        CrStaging stage = stagingService.determineStage(
                account,
                customer.getWarningLevel(),
                Boolean.TRUE.equals(account.getIsDebtRestructured()));

        BigDecimal pd = pdCalculationService.calculatePd(account, modelParams);
        BigDecimal ccf = ccfCalculationService.calculateCcf(account.getProductCode());
        EadCrmCalculationService.EadCrmResult eadCrm =
                eadCrmCalculationService.calculateEadCrm(account, ccf, modelParams);

        BigDecimal lgd = lgdCalculationService.calculateLgd(
                customerType.name(),
                eadCrm.getMajorCollateralType(),
                eadCrm.getTotalCollateralAmt().compareTo(BigDecimal.ZERO) > 0,
                modelParams);

        double maturityYears = resolveMaturityYears(account, baseDate);
        List<BigDecimal> marginalPds = lifetimePdService.generateMarginalPdCurve(
                pd,
                maturityYears,
                resolveRating(account),
                baseDate);

        ForwardLookingEclService.FlEclResult ecl = forwardLookingEclService.calculateWeightedEcl(
                stage,
                marginalPds,
                lgd,
                eadCrm.getEadStar(),
                modelParams.getDefaultDiscountRate(),
                baseDate.getYear());

        AllowanceEclResult result = AllowanceEclResult.builder()
                .id(account.getId())
                .baseDate(baseDate)
                .account(account)
                .staging(stage)
                .ead(eadCrm.getEadRaw())
                .eadStar(eadCrm.getEadStar())
                .appliedCcf(eadCrm.getAppliedCcf())
                .crmDeduction(eadCrm.getCrmDeduction())
                .pd(pd)
                .lgd(lgd)
                .expectedLoss(ecl.getWeightedEcl())
                .eclBoom(ecl.getEclBoom())
                .eclBase(ecl.getEclBase())
                .eclRecession(ecl.getEclRecession())
                .weightedEcl(ecl.getWeightedEcl())
                .status(CalculationStatus.COMPLETED)
                .calculationCompletedAt(LocalDateTime.now())
                .build();

        resultRepository.save(result);
        return result;
    }

    public Object getAllowanceSummary(LocalDate baseDate) {
        log.info("[IFRS9 Allowance] {} 기준 대손충당금 산출 요약 조회", baseDate);

        List<AllowanceEclResult> results = resultRepository.findAllByBaseDate(baseDate);
        if (results.isEmpty()) {
            return java.util.Map.of("message", "산출 결과가 없습니다.");
        }

        BigDecimal totalExposure = results.stream()
                .map(result -> result.getEadStar() != null ? result.getEadStar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAllowance = results.stream()
                .map(result -> result.getWeightedEcl() != null ? result.getWeightedEcl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double avgPd = results.stream()
                .mapToDouble(result -> result.getPd() != null ? result.getPd().doubleValue() : 0.0)
                .average()
                .orElse(0.0);
        java.util.Map<String, Long> stageSummary = results.stream()
                .collect(Collectors.groupingBy(result -> result.getStaging().name(), Collectors.counting()));

        return java.util.Map.of(
                "baseDate", baseDate,
                "totalExposure", totalExposure,
                "targetAllowanceAmount", totalAllowance,
                "avgPd", avgPd,
                "stageSummary", stageSummary);
    }

    private double resolveMaturityYears(CrAccount account, LocalDate baseDate) {
        if (account.getMaturityDate() == null) {
            return 2.5d;
        }
        long remainingDays = ChronoUnit.DAYS.between(baseDate, account.getMaturityDate());
        return Math.max(remainingDays / 365.0d, 1.0d);
    }

    private String resolveRating(CrAccount account) {
        if (account.getInternalRating() != null && !account.getInternalRating().isBlank()) {
            return account.getInternalRating();
        }
        return account.getCustomer().getInternalRating();
    }
}



