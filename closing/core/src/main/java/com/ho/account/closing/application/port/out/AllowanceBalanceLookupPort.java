package com.ho.account.closing.application.port.out;

import java.time.LocalDate;

/**
 * ECL 충당 전표 생성 전 실제 전기된 충당금의 거래통화·기능통화 잔액을 조회하는 포트.
 */
public interface AllowanceBalanceLookupPort {

    AllowanceBalance findCreditBalance(
            String allowanceAccountCode,
            String transactionCurrencyCode,
            String functionalCurrencyCode,
            LocalDate balanceDate);
}
