package com.ho.account.expenditure.domain;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "expenditure_details")
public class ExpenditureDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_resolution_id", nullable = false)
    private ExpenditureResolution expenditureResolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_code", nullable = false)
    private AccountSubject accountSubject; // 鍮꾩슜 怨꾩젙 (李⑤? 怨꾩젙)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_code", referencedColumnName = "businessPartnerCode")
    private BusinessPartner businessPartner; // 嫄곕옒泥?

    @Column(length = 200)
    private String description; // ?곸슂

    // Getter 諛?Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExpenditureResolution getExpenditureResolution() { return expenditureResolution; }
    public void setExpenditureResolution(ExpenditureResolution expenditureResolution) { this.expenditureResolution = expenditureResolution; }

    public AccountSubject getAccountSubject() { return accountSubject; }
    public void setAccountSubject(AccountSubject accountSubject) { this.accountSubject = accountSubject; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BusinessPartner getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(BusinessPartner businessPartner) { this.businessPartner = businessPartner; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
