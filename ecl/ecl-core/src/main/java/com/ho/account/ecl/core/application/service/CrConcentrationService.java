package com.ho.account.ecl.core.application.service;

import com.ho.account.ecl.core.application.port.out.CrAccountRepository;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [Service] 대손충당금(IFRS9) 집중도 분석 서비스
 * 특정 산업군에 대출이 과도하게 쏠려 있는지(Concentration Risk) 분석합니다.
 *
 * 💡 [초보자를 위한 가이드]
 * 은행이 특정 산업(예: 부동산, 건설)에만 돈을 너무 많이 빌려주면, 
 * 그 산업이 어려워질 때 은행 전체가 휘청거릴 수 있습니다. (계란을 한 바구니에 담지 말라는 원리!)
 * 이 프로그램은 "우리 은행 돈이 어디에 얼마나 가 있나?"를 산업별로 계산해주는 돋보기 역할을 하며,
 * 특정 산업의 비중이 너무 높으면 경고(Limit Exceeded)를 주는 기능도 포함하고 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrConcentrationService {

    private final CrAccountRepository accountRepository;

    /**
     * 산업별 익스포저(EAD) 및 비중 분석 결과를 반환합니다.
     * 
     * 💡 [Java 기초 설명]
     * - List<CrAccount>: 계좌들의 목록입니다. (JS의 Array와 비슷합니다)
     * - stream(): 목록에서 데이터를 하나씩 꺼내서 처리하는 '흐름'을 만듭니다.
     * - Collectors.groupingBy: 특정 기준(산업 코드)으로 데이터를 그룹 짓습니다. (JS의 _.groupBy 와 비슷합니다)
     */
    public Map<String, Object> analyzeIndustryConcentration(String baseDate) {
        log.info("🔍 [집중도 분석] {} 기준 산업별 리스크 분포 분석 중...", baseDate);

        // 1. 모든 계좌 데이터를 가져옵니다.
        List<CrAccount> accounts = accountRepository.findByIsActiveTrue();

        if (accounts.isEmpty()) {
            return Map.of("baseDate", baseDate, "items", List.of());
        }

        // 2. 산업군별로 익스포저(EAD)를 합산합니다.
        // groupingBy를 사용하여 industryCode가 같은 계좌끼리 묶습니다.
        Map<String, BigDecimal> industrySummary = accounts.stream()
                .filter(a -> a.getCustomer() != null && a.getCustomer().getIndustryCode() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCustomer().getIndustryCode(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                CrAccount::getOutstandingAmount, // 현재 잔액 기준 (EAD 대용)
                                BigDecimal::add
                        )
                ));

        // 3. 전체 합계를 구합니다.
        BigDecimal totalExposure = industrySummary.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 결과를 예쁘게 정리합니다. (각 산업별 비중 계산)
        List<Map<String, Object>> items = industrySummary.entrySet().stream()
                .map(entry -> {
                    BigDecimal amount = entry.getValue();
                    BigDecimal weight = amount.divide(totalExposure, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("industry", entry.getKey());
                    item.put("amount", amount);
                    item.put("weight", weight);
                    item.put("isLimitExceeded", weight.compareTo(new BigDecimal("20")) > 0);
                    return item;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")))
                .toList();

        return Map.of(
                "baseDate", baseDate,
                "totalExposure", totalExposure,
                "items", items
        );
    }

    /**
     * 특정 산업군의 상세 리스크 내역(주요 차주 리스트)을 반환합니다.
     * 대시보드에서 특정 산업을 클릭했을 때 나타나는 '드릴다운(Drill-down)' 화면용 데이터입니다.
     */
    public Map<String, Object> getIndustryDetails(String industryCode) {
        log.info("🔍 [집중도 상세] 산업코드 {} 상세 분석 중...", industryCode);

        // 1. 해당 산업군에 속한 계좌들만 필터링합니다.
        List<CrAccount> industryAccounts = accountRepository.findByIsActiveTrue().stream()
                .filter(a -> a.getCustomer() != null && industryCode.equals(a.getCustomer().getIndustryCode()))
                .collect(Collectors.toList());

        // 2. 차주별로 익스포저를 합계하여 '큰 손' 차주들을 찾아냅니다.
        Map<String, BigDecimal> customerSummary = industryAccounts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCustomer().getCustomerName(),
                        Collectors.reducing(BigDecimal.ZERO, CrAccount::getOutstandingAmount, BigDecimal::add)
                ));

        // 3. 상위 10개 차주만 리스트로 변환합니다.
        List<Map<String, Object>> topCustomers = customerSummary.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("customerName", entry.getKey());
                    map.put("totalAmount", entry.getValue());
                    return map;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")))
                .limit(10)
                .collect(Collectors.toList());

        return Map.of(
                "industryCode", industryCode,
                "totalAmount", industryAccounts.stream().map(CrAccount::getOutstandingAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                "accountCount", industryAccounts.size(),
                "topCustomers", topCustomers
        );
    }
}
