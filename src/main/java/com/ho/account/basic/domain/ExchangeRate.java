package com.ho.account.basic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 환율 정보 엔티티.
 * 특정 일자에 적용되는 통화 간의 환율을 관리합니다.
 */
@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency_code", "to_currency_code", "effective_date"}))
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_currency_code", referencedColumnName = "code", nullable = false)
    private Currency fromCurrency; // 기준 통화 (예: USD)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_currency_code", referencedColumnName = "code", nullable = false)
    private Currency toCurrency; // 대상 통화 (예: KRW)

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate; // 환율 (예: 1 USD = 1300 KRW)

    @Column(nullable = false)
    private LocalDate effectiveDate; // 환율 적용 시작일

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(Currency fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(Currency toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
