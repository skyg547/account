package com.ho.account.ecl.core.application.port.out;

import com.ho.account.shared.finance.enums.CrStaging;
import com.ho.account.shared.finance.enums.CurrencyCode;
import com.ho.account.shared.finance.enums.CustomerType;
import com.ho.account.shared.finance.enums.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ECL 계산이 Account Mart에서 읽는 기술 독립적 입력 스냅샷.
 */
public record AllowanceInputPositionSnapshot(
        LocalDate baseDate,
        String accountNo,
        String customerCode,
        String customerName,
        CustomerType customerType,
        Boolean isSme,
        String countryCode,
        String productCode,
        ProductCategory productCategory,
        CurrencyCode currency,
        BigDecimal outstandingAmount,
        BigDecimal limitAmount,
        BigDecimal interestRate,
        LocalDate openDate,
        LocalDate maturityDate,
        String repaymentMethod,
        Integer gracePeriod,
        Integer repaymentFrequency,
        String internalRating,
        String industryCode,
        String warningLevel,
        Boolean isDebtRestructured,
        Integer delinquentDays,
        CrStaging staging,
        String branchCode,
        String businessUnitCode) {
}
