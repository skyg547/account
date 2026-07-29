package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Exchange rate master data.
 * Stores effective rates between two currencies.
 */
@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency_code", "to_currency_code", "effective_date"}))
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency_code", nullable = false, length = 3)
    private String fromCurrencyCode; // Source currency, for example USD

    @Column(name = "to_currency_code", nullable = false, length = 3)
    private String toCurrencyCode; // Target currency, for example KRW

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate; // Exchange rate, for example 1 USD = 1300 KRW

    @Column(nullable = false)
    private LocalDate effectiveDate; // Effective start date

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

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
