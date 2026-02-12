package com.ho.account.basic.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime 추가

/**
 * 계정과목(Chart of Accounts) 마스터 엔티티.
 * 계층 구조, 유효 기간, 재무제표 매핑 등을 포함하여 전체 회계 시스템의 기반을 정의합니다.
 */
@Entity
@Table(name = "account_subjects")
public class AccountSubject {

    /**
     * 계정 코드 (Primary Key)
     */
    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일시

    @Column(length = 50)
    private String auditUser; // 감사 사용자

    /**
     * 계정명
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 상위 계정 코드 (계층 구조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code")
    private AccountSubject parent;

    /**
     * 계정 대분류 (BS, IS, CF 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AccountType accountType;

    /**
     * 계정 잔액 타입 (차변/대변)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BalanceType balanceType;

        /**
         * 재무제표 표시 라인 (재무상태표, 손익계산서 등의 항목)
         */
        @Column(length = 100)
        private String financialReportMappingCode;
    
        /**
         * 감독회계 보고서 매핑 코드
         */
        @Column(length = 100)
        private String regulatoryMappingCode;
    
        /**
         * 미결(채권/채무) 관리 여부
         */
        @Column(nullable = false)
        private boolean unsettled;
    
        /**
         * 유효 시작일 (SCD2)
         */
        @Column(nullable = false)
        private LocalDate validFrom;
    
        /**
         * 유효 종료일 (SCD2)
         */
        @Column(nullable = false)
        private LocalDate validTo;
        
        // --- 기존 필드 유지 또는 통합 ---
        /**
         * 고정자산 계정 여부
         */
        @Column(nullable = false)
        private boolean fixedAsset;
    
        // --- Enum Definitions ---
        public enum AccountType {
            ASSETS, LIABILITIES, EQUITY, REVENUE, EXPENSES
        }
    
        public enum BalanceType {
            DEBIT, CREDIT
        }
    
        // --- Getters and Setters ---
    
        public String getCode() {
            return code;
        }
    
        public void setCode(String code) {
            this.code = code;
        }
    
        public String getName() {
            return name;
        }
    
        public void setName(String name) {
            this.name = name;
        }
    
        public AccountSubject getParent() {
            return parent;
        }
    
        public void setParent(AccountSubject parent) {
            this.parent = parent;
        }
    
        public AccountType getAccountType() {
            return accountType;
        }
    
        public void setAccountType(AccountType accountType) {
            this.accountType = accountType;
        }
    
        public BalanceType getBalanceType() {
            return balanceType;
        }
    
        public void setBalanceType(BalanceType balanceType) {
            this.balanceType = balanceType;
        }
    
        public String getFinancialReportMappingCode() {
            return financialReportMappingCode;
        }
    
        public void setFinancialReportMappingCode(String financialReportMappingCode) {
            this.financialReportMappingCode = financialReportMappingCode;
        }
    
        public String getRegulatoryMappingCode() {
            return regulatoryMappingCode;
        }
    
        public void setRegulatoryMappingCode(String regulatoryMappingCode) {
            this.regulatoryMappingCode = regulatoryMappingCode;
        }
        
        public boolean isUnsettled() {
            return unsettled;
        }
    public void setUnsettled(boolean unsettled) {
        this.unsettled = unsettled;
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

    public boolean isFixedAsset() {
        return fixedAsset;
    }

    public void setFixedAsset(boolean fixedAsset) {
        this.fixedAsset = fixedAsset;
    }

    // 추가된 필드의 Getter and Setter
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

