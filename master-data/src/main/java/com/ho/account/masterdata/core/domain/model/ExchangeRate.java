package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ?섏쑉 ?뺣낫 ?뷀떚??
 * ?뱀젙 ?쇱옄???곸슜?섎뒗 ?듯솕 媛꾩쓽 ?섏쑉??愿由ы빀?덈떎.
 */
@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency_code", "to_currency_code", "effective_date"}))
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_currency_code", referencedColumnName = "currency_code", nullable = false)
    private Currency fromCurrency; // 湲곗? ?듯솕 (?? USD)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_currency_code", referencedColumnName = "currency_code", nullable = false)
    private Currency toCurrency; // ????듯솕 (?? KRW)

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate; // ?섏쑉 (?? 1 USD = 1300 KRW)

    @Column(nullable = false)
    private LocalDate effectiveDate; // ?섏쑉 ?곸슜 ?쒖옉??

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getter 諛?Setter
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
