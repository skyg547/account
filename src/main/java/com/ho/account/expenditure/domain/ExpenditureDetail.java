package com.ho.account.expenditure.domain;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Customer;
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
    private AccountSubject accountSubject; // 비용 계정 (차변 계정)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_code", referencedColumnName = "customerCode")
    private Customer customer; // 거래처

    @Column(length = 200)
    private String description; // 적요

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExpenditureResolution getExpenditureResolution() { return expenditureResolution; }
    public void setExpenditureResolution(ExpenditureResolution expenditureResolution) { this.expenditureResolution = expenditureResolution; }

    public AccountSubject getAccountSubject() { return accountSubject; }
    public void setAccountSubject(AccountSubject accountSubject) { this.accountSubject = accountSubject; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
