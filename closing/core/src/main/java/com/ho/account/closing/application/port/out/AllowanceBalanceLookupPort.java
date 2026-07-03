package com.ho.account.closing.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ECL 충당 전표를 만들기 전에 기존 대손충당금 GL 잔액을 조회하는 포트.
 */
public interface AllowanceBalanceLookupPort {

    BigDecimal findCreditEndingBalance(String allowanceAccountCode, String currencyCode, LocalDate balanceDate);
}