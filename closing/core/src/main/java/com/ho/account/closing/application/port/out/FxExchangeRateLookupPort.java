package com.ho.account.closing.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * FX 평가에 필요한 환율을 조회하는 기술 독립 포트.
 *
 * <p>초보자 설명: core는 환율이 JPA Repository에서 오는지, 외부 API에서 오는지 몰라야 한다.
 * 이 포트가 그 경계를 만들고 batch/infrastructure adapter가 실제 조회 기술을 담당한다.
 */
public interface FxExchangeRateLookupPort {

    Optional<BigDecimal> findRate(String fromCurrencyCode, String toCurrencyCode, LocalDate effectiveDate);
}