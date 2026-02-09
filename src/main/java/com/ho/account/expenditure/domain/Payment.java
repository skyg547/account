package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expenditure_resolution_id", nullable = false)
    private ExpenditureResolution expenditureResolution;

    @Column(nullable = false)
    private LocalDateTime paymentDate; // 실제 지급 일시

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String paymentMethod; // TRANSFER(이체), CASH(현금), CARD(카드)

    @Column(length = 20)
    private String status; // COMPLETED, FAILED

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExpenditureResolution getExpenditureResolution() { return expenditureResolution; }
    public void setExpenditureResolution(ExpenditureResolution expenditureResolution) { this.expenditureResolution = expenditureResolution; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
