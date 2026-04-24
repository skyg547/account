package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 통화 정보 엔티티 (ISO 4217)
 */
@Entity
@Table(name = "currencies")
public class Currency {

    @Id
    @Column(name = "currency_code", length = 3)
    private String currencyCode; // ISO 4217 (예: KRW, USD, EUR)

    @Column(nullable = false, length = 50)
    private String currencyName;

    @Column(length = 10)
    private String symbol; // 통화 기호 (예: ₩, $)

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.validFrom == null)
            this.validFrom = LocalDate.now();
        if (this.validTo == null)
            this.validTo = LocalDate.of(9999, 12, 31);
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 및 Setter
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Deprecated
    public String getCode() {
        return getCurrencyCode();
    }

    @Deprecated
    public void setCode(String code) {
        setCurrencyCode(code);
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    @Deprecated
    public String getName() {
        return getCurrencyName();
    }

    @Deprecated
    public void setName(String name) {
        setCurrencyName(name);
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
