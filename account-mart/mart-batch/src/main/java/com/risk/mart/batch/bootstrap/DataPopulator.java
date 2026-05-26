package com.risk.mart.batch.bootstrap;

import com.risk.common.enums.*;
import com.risk.mart.core.domain.ods.common.entity.OdsCustomerMst;
import com.risk.mart.core.domain.ods.common.entity.OdsProductMst;
import com.risk.mart.core.domain.ods.loan.entity.OdsAccountLedger;
import com.risk.mart.core.domain.ods.loan.entity.OdsCollateralMst;
import com.risk.mart.core.domain.ods.common.repository.OdsCustomerMstRepository;
import com.risk.mart.core.domain.ods.common.repository.OdsProductMstRepository;
import com.risk.mart.core.domain.ods.loan.repository.OdsAccountLedgerRepository;
import com.risk.mart.core.domain.ods.loan.repository.OdsCollateralMstRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * [Simulator] 엔터프라이즈 ODS 데이터 생성기 (Populator).
 * 신용 리스크 및 금리 리스크 산출 테스트를 위한 고정밀 시뮬레이션 데이터를 생성합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mart.batch.bootstrap", name = "enabled", havingValue = "true")
public class DataPopulator {

    private final OdsAccountLedgerRepository accountRepository;
    private final OdsCustomerMstRepository customerRepository;
    private final OdsProductMstRepository productRepository;
    private final OdsCollateralMstRepository collateralRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (accountRepository.count() > 0) {
                log.info("ℹ️ [데이터 생성] 기존 ODS 데이터가 존재하여 생성을 건너뜁니다.");
                return;
            }

            log.info("🚀 [데이터 생성] 고정밀 ODS 시뮬레이션 데이터 생성을 시작합니다. (H2/PostgreSQL 호환)");
            Random random = new Random(42);

            // 1. 상품 마스터 생성 (표준 상품군 정의)
            List<OdsProductMst> productMasts = new ArrayList<>();
            productMasts.add(OdsProductMst.builder()
                    .productCode("RETAIL_LOAN").productName("개인 신용대출").productCategory("LOAN")
                    .rateType("FLOATING").paymentFreq(1).isExcluded(false).defaultCcf(BigDecimal.ZERO).build());

            productMasts.add(OdsProductMst.builder()
                    .productCode("CORP_LOAN").productName("기업 운전자금대출").productCategory("LOAN")
                    .rateType("FLOATING").paymentFreq(3).isExcluded(false).defaultCcf(BigDecimal.ZERO).build());

            productMasts.add(OdsProductMst.builder()
                    .productCode("CREDIT_CARD").productName("프리미엄 신용카드").productCategory("CARD")
                    .rateType("FLOATING").paymentFreq(1).isExcluded(false).defaultCcf(new BigDecimal("0.2000"))
                    .build());

            productMasts.add(OdsProductMst.builder()
                    .productCode("CORP_LIMIT").productName("기업 약대한도").productCategory("OFF_BALANCE")
                    .rateType("FLOATING").paymentFreq(1).isExcluded(false).defaultCcf(new BigDecimal("0.5000"))
                    .build());

            productRepository.saveAll(productMasts);

            // 2. 가상 고객 생성 (개인/SME/대기업 분류)
            List<OdsCustomerMst> customers = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                boolean isSme = (i % 7 == 0);
                CustomerType type = isSme ? CustomerType.SME
                        : (i % 5 == 0 ? CustomerType.CORPORATE : CustomerType.RETAIL);
                customers.add(OdsCustomerMst.builder()
                        .customerCode("CUST-" + (1000 + i))
                        .customerName("가상고객_" + i)
                        .customerType(type.name())
                        .ratingCode(String.valueOf(random.nextInt(8) + 1))
                        .industryCode(type == CustomerType.RETAIL ? "RETAIL" : "IND-" + (random.nextInt(10) + 1))
                        .countryCode(random.nextDouble() > 0.9 ? "US" : "KR")
                        .build());
            }
            customerRepository.saveAll(customers);

            // 3. 계정 원장 및 담보 데이터 생성
            List<OdsAccountLedger> accounts = new ArrayList<>();
            List<OdsCollateralMst> collaterals = new ArrayList<>();

            for (int i = 1; i <= 2000; i++) {
                OdsCustomerMst cust = customers.get(random.nextInt(customers.size()));
                OdsProductMst prod = productMasts.get(random.nextInt(productMasts.size()));

                BigDecimal limitAmt = BigDecimal.valueOf(100_000_000 + random.nextInt(900_000_000));
                BigDecimal outstdAmt = limitAmt.multiply(BigDecimal.valueOf(0.1 + random.nextDouble() * 0.9));
                Integer ddays = (random.nextInt(100) > 97) ? random.nextInt(120) : 0;

                String accNo = String.format("ACC-%06d", i);
                accounts.add(OdsAccountLedger.builder()
                        .accountNo(accNo)
                        .customerCode(cust.getCustomerCode())
                        .productCode(prod.getProductCode())
                        .currency("KRW")
                        .limitAmount(limitAmt)
                        .outstandingAmount(outstdAmt)
                        .openDate(LocalDate.now().minusMonths(random.nextInt(48)))
                        .maturityDate(LocalDate.now().plusMonths(6 + random.nextInt(120)))
                        .interestRate(BigDecimal.valueOf(3.5 + random.nextDouble() * 4.0)) // 3.5% ~ 7.5%
                        .baseRateCode(i % 3 == 0 ? "CD_3M" : "KORIBOR_3M")
                        .spread(BigDecimal.valueOf(0.5 + random.nextDouble() * 2.5))
                        .nextResetDate(LocalDate.now().plusDays(random.nextInt(90)))
                        .delinquentDays(ddays)
                        .repaymentMethod(i % 3 == 0 ? "BULLET" : (i % 3 == 1 ? "EQUAL_PRINCIPAL" : "EQUAL_INST"))
                        .isActive(i % 50 != 0)
                        .build());

                // 담보 설정 (60% 확률로 담보 설정)
                if (random.nextDouble() > 0.4) {
                    BigDecimal collAmt = outstdAmt.multiply(new BigDecimal("1.2"));
                    collaterals.add(OdsCollateralMst.builder()
                            .collateralId("COLL-" + i)
                            .customerCode(cust.getCustomerCode())
                            .collateralType(random.nextDouble() > 0.8 ? "CASH_DEPOSIT" : "REAL_ESTATE")
                            .collateralAmount(collAmt)
                            .haircutRatio(new BigDecimal("0.3"))
                            .build());
                }
            }

            accountRepository.saveAll(accounts);
            collateralRepository.saveAll(collaterals);
            log.info("✅ [데이터 생성] 고정밀 ODS 데이터 생성 완료: {}개 계좌가 생성되었습니다.", accounts.size());
        };
    }
}
