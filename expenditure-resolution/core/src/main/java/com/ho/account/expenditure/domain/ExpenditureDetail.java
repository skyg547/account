package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * [ExpenditureDetail] 도메인 엔티티.
 * 지출 결의서의 세부 내역(라인 아이템)을 관리합니다.
 */
@Entity
@Table(name = "expenditure_details")
public class ExpenditureDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_resolution_id", nullable = false)
    private ExpenditureResolution expenditureResolution;

    /**
     * 비용 계정과목 코드 (차변)
     */
    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * 거래처 코드 (ID 참조)
     */
    @Column(name = "business_partner_code", length = 20)
    private String businessPartnerCode;

    @Column(length = 200)
    private String description; // 적요

    protected ExpenditureDetail() {}

    /**
     * 지출 결의 상세 생성을 위한 정적 팩토리 메서드.
     */
    public static ExpenditureDetail create(String accountCode, BigDecimal amount, String businessPartnerCode, String description) {
        ExpenditureDetail detail = new ExpenditureDetail();
        detail.accountCode = accountCode;
        detail.amount = amount;
        detail.businessPartnerCode = businessPartnerCode;
        detail.description = description;
        return detail;
    }

    // Getter 및 Setter
    public Long getId() { return id; }

    public ExpenditureResolution getExpenditureResolution() { return expenditureResolution; }
    public void setExpenditureResolution(ExpenditureResolution expenditureResolution) { this.expenditureResolution = expenditureResolution; }

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBusinessPartnerCode() { return businessPartnerCode; }
    public void setBusinessPartnerCode(String businessPartnerCode) { this.businessPartnerCode = businessPartnerCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
