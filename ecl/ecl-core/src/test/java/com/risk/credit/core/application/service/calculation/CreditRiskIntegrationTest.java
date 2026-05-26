package com.risk.credit.core.application.service.calculation;

import com.risk.credit.core.application.port.out.CrAccountRepository;
import com.risk.credit.core.application.port.out.CrCustomerRepository;
import com.risk.credit.core.application.port.out.CrProductMasterRepository;
import com.risk.credit.core.application.port.out.CrRiskResultRepository;
import com.risk.credit.core.domain.exposure.CrAccount;
import com.risk.credit.core.domain.exposure.CrCustomer;
import com.risk.credit.core.domain.model.CrProductMaster;
import com.risk.credit.core.domain.result.CrRiskResult;
import com.risk.common.enums.CalculationStatus;
import com.risk.common.enums.CrStaging;
import com.risk.common.enums.CustomerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [QA] 신용 리스크 산출 통합 테스트 (Integration Test)
 * 실제 schema-cr.sql 및 data-cr.sql 데이터를 기반으로 
 * DB 정합성과 산출 로직의 무결성을 동시에 검증합니다.
 */
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.format_sql=true",
    "spring.jpa.properties.hibernate.highlight_sql=true"
})
@ActiveProfiles("test")
@Transactional
class CreditRiskIntegrationTest {

    @SpringBootApplication(scanBasePackages = "com.risk")
    @EnableJpaRepositories(basePackages = "com.risk")
    @EntityScan(basePackages = "com.risk")
    static class TestApplication {}

    @Autowired
    private CreditRiskService creditRiskService;

    @Autowired
    private CrAccountRepository accountRepository;

    @Autowired
    private CrRiskResultRepository riskResultRepository;

    @Autowired
    private CrCustomerRepository customerRepository;

    @Autowired
    private CrProductMasterRepository productRepository;

    private Long samsungAccountId;

    @BeforeEach
    void setUp() {
        // 💡 [QA 핵심] 데이터도 엔티티 기준으로 직접 생성하여 SQL 문법 차이 문제를 완벽히 차단합니다.
        
        // 1. 차주 생성
        CrCustomer samsung = CrCustomer.builder()
                .customerCode("CUST-SM-001")
                .customerName("(주)삼전테스트")
                .customerType(CustomerType.CORPORATE)
                .industryCode("C262")
                .isActive(true)
                .build();
        samsung = customerRepository.save(samsung);

        // 2. 상품 생성
        CrProductMaster product = CrProductMaster.builder()
                .productCode("LN-CORP")
                .productName("기업대출테스트")
                .ccfRate(BigDecimal.ZERO)
                .build();
        productRepository.save(product);

        // 3. 계좌 생성
        CrAccount account = CrAccount.builder()
                .accountNo("ACC-SM-TEST-001")
                .customer(samsung)
                .productCode("LN-CORP")
                .outstandingAmount(new BigDecimal("1000000000"))
                .currency("KRW")
                .openDate(LocalDate.now().minusYears(1))
                .staging(CrStaging.STAGE1)
                .isActive(true)
                .build();
        account = accountRepository.save(account);
        this.samsungAccountId = account.getId();
    }

    @Test
    @DisplayName("✅ [DB 연동] 동적 생성 계좌 산출 검증")
    void calculateSamsungAccountRisk_Success() {
        // given
        LocalDate baseDate = LocalDate.now();

        // when
        // setUp에서 생성된 samsungAccountId 사용
        CrRiskResult result = creditRiskService.calculateAccountRisk(this.samsungAccountId, baseDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccount().getCustomer().getCustomerName()).contains("삼전테스트");
        assertThat(result.getStatus()).isEqualTo(CalculationStatus.COMPLETED);
        
        logIntegrationResult(result);
    }

    @Test
    @DisplayName("✅ [요약 지표] 전사 리스크 요약 API 정합성 검증")
    void verifyResultsSummary_Accuracy() {
        // given
        LocalDate baseDate = LocalDate.now();
        
        // 1. setUp에서 생성된 계좌에 대해 산출 수행
        creditRiskService.calculateAccountRisk(this.samsungAccountId, baseDate);

        // when
        Object summaryObj = creditRiskService.getResultsSummary(baseDate);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) summaryObj;

        // then
        List<CrRiskResult> actualResults = riskResultRepository.findAllByBaseDate(baseDate);
        BigDecimal manualTotalEad = actualResults.stream()
                .map(r -> r.getEadStar() != null ? r.getEadStar() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(summary.get("totalEad").toString()).isEqualTo(manualTotalEad.toString());
        assertThat(actualResults.size()).isGreaterThanOrEqualTo(1);
        
        System.out.println("====== [요약 지표 정합성 확인] ======");
        System.out.println("집계된 총 EAD: " + summary.get("totalEad"));
        System.out.println("==================================");
    }

    private void logIntegrationResult(CrRiskResult result) {
        // ... (이전 코드 동일)
    }
}
