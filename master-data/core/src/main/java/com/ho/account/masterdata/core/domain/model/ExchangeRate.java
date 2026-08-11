package com.ho.account.masterdata.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 환율(Exchange Rate) 도메인 모델.
 * 두 통화 간의 적용 환율 정보를 관리합니다.
 * 
 * 🐣 [DDD & Pure POJO 원칙 교육적 주석]
 * 1. Pure POJO 원칙:
 *    환율 도메인 모델은 JPA 기술에 의존하지 않는 순수한 자바 객체입니다.
 * 2. 도메인-영속성 모델 분리:
 *    테이블 매핑 및 제약조건은 ExchangeRateEntity에서 처리하고, Data Mapper를 통해 매핑합니다.
 */
public class ExchangeRate {

    private Long id;
    private String fromCurrencyCode; // Source currency, for example USD
    private String toCurrencyCode; // Target currency, for example KRW
    private BigDecimal rate; // Exchange rate, for example 1 USD = 1300 KRW
    private LocalDate effectiveDate; // Effective start date
    private LocalDateTime createdAt;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromCurrencyCode() {
        return fromCurrencyCode;
    }

    public void setFromCurrencyCode(String fromCurrencyCode) {
        this.fromCurrencyCode = normalizeCurrencyCode(fromCurrencyCode, "From currency code");
    }

    public String getToCurrencyCode() {
        return toCurrencyCode;
    }

    public void setToCurrencyCode(String toCurrencyCode) {
        this.toCurrencyCode = normalizeCurrencyCode(toCurrencyCode, "To currency code");
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("Exchange rate must be greater than zero.");
        }
        this.rate = rate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw new IllegalArgumentException("Exchange rate effective date is required.");
        }
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private String normalizeCurrencyCode(String currencyCode, String fieldName) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String normalized = currencyCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.length() != 3) {
            throw new IllegalArgumentException(fieldName + " must be a 3-letter ISO code.");
        }
        return normalized;
    }
}

