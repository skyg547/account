package com.ho.account.masterdata.core.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 통화 정보 도메인 모델 (ISO 4217)
 * 
 * 🐣 [DDD & Pure POJO 원칙 교육적 주석]
 * 1. Pure POJO 원칙:
 *    도메인 모델은 특정 영속성 프레임워크 어노테이션에 의존하지 않는 순수한 자바 객체입니다.
 * 2. 도메인-영속성 모델 분리:
 *    DB 테이블 구조 및 JPA 어노테이션은 infrastructure 패키지의 CurrencyEntity에서 정의하며,
 *    Data Mapper를 통해 뷰 및 데이터 엑세스 레이어와의 디커플링을 유지합니다.
 */
public class Currency {

    private Long id;
    private String currencyCode; // ISO 4217 (예: KRW, USD, EUR)
    private String currencyName;
    private String symbol; // 통화 기호 (예: ₩, $)
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public boolean isValid(LocalDate date) {
        return date != null
                && (date.isEqual(validFrom) || date.isAfter(validFrom))
                && (date.isEqual(validTo) || date.isBefore(validTo));
    }

    public void terminate(LocalDate endDate) {
        this.validTo = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 및 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}

