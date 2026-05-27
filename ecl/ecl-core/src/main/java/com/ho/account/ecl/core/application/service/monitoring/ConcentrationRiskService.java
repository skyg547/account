package com.ho.account.ecl.core.application.service.monitoring;

import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [CRM] 집중 리스크 (Concentration Risk) 관리 서비스
 *
 * HHI(Herfindahl-Hirschman Index)를 활용하여 포트폴리오의 집중도를 측정합니다.
 *
 * [초보자를 위한 개념 설명]
 * 집중 리스크는 특정 업종이나 차주에게 리스크 노출(Exposure)이 과도하게 몰려 있는 상태를 의미합니다.
 * 같은 규모의 자산이라도 여러 곳에 분산되어 있으면 위험이 줄고, 한 곳에 몰리면 위험이 커집니다.
 * HHI는 0~10,000 사이의 숫자로 이 '몰림 정도'를 수치화한 것입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConcentrationRiskService {

    private final CrAccountRepository accountRepository;

    /**
     * 특정 업종군에 대한 집중도(HHI)를 산출합니다.
     */
    public BigDecimal calculateIndustryHhi() {
        List<CrAccount> allAccounts = accountRepository.findByIsActiveTrue();
        if (allAccounts.isEmpty())
            return BigDecimal.ZERO;

        BigDecimal totalExposure = allAccounts.stream()
                .map(CrAccount::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> industryExposure = allAccounts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCustomer().getIndustryCode() != null ? a.getCustomer().getIndustryCode() : "UNKNOWN",
                        Collectors.reducing(BigDecimal.ZERO, CrAccount::getOutstandingAmount, BigDecimal::add)));

        return calculateHhi(industryExposure, totalExposure);
    }

    /**
     * 업종 집중도 상태를 반환합니다.
     */
    public String getIndustryConcentrationStatus(BigDecimal hhi) {
        if (hhi.compareTo(new BigDecimal("1500")) < 0)
            return "LOW (분산)";
        if (hhi.compareTo(new BigDecimal("2500")) < 0)
            return "MEDIUM (집중 주의)";
        return "HIGH (고집중 위험)";
    }

    /**
     * 차주(Counterparty)별 집중도(HHI)를 산출합니다.
     */
    public BigDecimal calculateCounterpartyHhi() {
        List<CrAccount> allAccounts = accountRepository.findByIsActiveTrue();
        if (allAccounts.isEmpty())
            return BigDecimal.ZERO;

        BigDecimal totalExposure = allAccounts.stream()
                .map(CrAccount::getOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, BigDecimal> customerExposure = allAccounts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCustomer().getId(),
                        Collectors.reducing(BigDecimal.ZERO, CrAccount::getOutstandingAmount, BigDecimal::add)));

        return calculateHhi(customerExposure, totalExposure);
    }

    /**
     * 차주 집중도 상태를 반환합니다.
     */
    public String getCounterpartyConcentrationStatus(BigDecimal hhi) {
        if (hhi.compareTo(new BigDecimal("100")) < 0)
            return "LOW (분산)";
        if (hhi.compareTo(new BigDecimal("500")) < 0)
            return "MEDIUM (집중 주의)";
        return "HIGH (고집중 위험)";
    }

    /**
     * HHI 수식을 적용하여 집중도 지수를 계산합니다.
     */
    private BigDecimal calculateHhi(Map<?, BigDecimal> groupExposure, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;

        BigDecimal hhiSum = BigDecimal.ZERO;
        for (BigDecimal exposure : groupExposure.values()) {
            BigDecimal share = exposure.divide(total, 6, RoundingMode.HALF_UP);
            hhiSum = hhiSum.add(share.multiply(share));
        }

        return hhiSum.multiply(new BigDecimal("10000")).setScale(2, RoundingMode.HALF_UP);
    }
}
