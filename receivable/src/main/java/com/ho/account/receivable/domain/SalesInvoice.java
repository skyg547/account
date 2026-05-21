package com.ho.account.receivable.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * [SalesInvoice] 도메인 엔티티.
 * 고객에게 발행된 매출 및 청구 정보를 관리하며, 대금 수금 상태와 연령(Aging) 분석 기능을 제공합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 회사가 고객에게 물건을 팔고 "돈 내세요~" 하고 보내는 '청구서'입니다.
 * 청구서가 발행(ISSUED)되면 짝꿍으로 받을 권리인 Receivable(매출채권)이 생깁니다.
 * 이 클래스는 청구서 자체의 내용(금액, 세금, 만기일)을 보관하며, 나중에 
 * "만기일이 지났는데 얼마나 연체됐지?(AgingDays)" 계산하는 기능을 스스로 가지고 있습니다.
 */
@Entity
@Table(name = "sales_invoices")
public class SalesInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String invoiceNo; // 인보이스 번호

    /**
     * 고객(거래처) 코드 (ID 참조로 변경하여 모듈 간 결합도 제거)
     */
    @Column(name = "customer_code", length = 20, nullable = false)
    private String customerCode;

    @Column(nullable = false)
    private LocalDate issueDate; // 발행일

    @Column(nullable = false)
    private LocalDate dueDate; // 만기일(수금 예정일)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // 총액 (공급가액 + 세액)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount; // 세액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount; // 공급가액

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SalesInvoiceStatus status; // 인보이스 상태 (ISSUED, PAID, PARTIAL_PAID, OVERDUE, CANCELLED)

    @Column(length = 500)
    private String description; // 설명

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SalesInvoice() {}

    /**
     * 매출 인보이스 생성을 위한 정적 팩토리 메서드.
     */
    public static SalesInvoice create(String invoiceNo, String customerCode, LocalDate issueDate, LocalDate dueDate,
                                     BigDecimal netAmount, BigDecimal taxAmount, String createdBy) {
        return create(invoiceNo, customerCode, issueDate, dueDate, netAmount, taxAmount, createdBy, null);
    }

    public static SalesInvoice create(String invoiceNo, String customerCode, LocalDate issueDate, LocalDate dueDate,
                                     BigDecimal netAmount, BigDecimal taxAmount, String createdBy, String description) {
        SalesInvoice invoice = new SalesInvoice();
        invoice.invoiceNo = invoiceNo;
        invoice.customerCode = customerCode;
        invoice.issueDate = issueDate;
        invoice.dueDate = dueDate;
        invoice.netAmount = netAmount;
        invoice.taxAmount = taxAmount;
        invoice.totalAmount = netAmount.add(taxAmount);
        invoice.createdBy = createdBy;
        invoice.status = SalesInvoiceStatus.ISSUED;
        invoice.description = description;
        return invoice;
    }

    /**
     * 채권 연령(Aging)을 계산합니다. (만기일 기준 경과 일수)
     */
    public long getAgingDays(LocalDate baseDate) {
        if (this.status == SalesInvoiceStatus.PAID || this.status == SalesInvoiceStatus.CANCELLED) {
            return 0;
        }
        return ChronoUnit.DAYS.between(this.dueDate, baseDate);
    }

    /**
     * 인보이스 상태를 수금 완료로 변경합니다.
     */
    public void markAsPaid() {
        this.status = SalesInvoiceStatus.PAID;
    }

    /**
     * 인보이스 상태를 연체로 변경합니다.
     */
    public void markAsOverdue() {
        if (this.status != SalesInvoiceStatus.PAID && this.status != SalesInvoiceStatus.CANCELLED) {
            this.status = SalesInvoiceStatus.OVERDUE;
        }
    }

    /**
     * 채권(Receivable)의 상태에 따라 인보이스 상태를 업데이트합니다.
     */
    public void updateStatusFromReceivable(ReceivableStatus receivableStatus) {
        if (receivableStatus == ReceivableStatus.PAID) {
            this.status = SalesInvoiceStatus.PAID;
        } else if (receivableStatus == ReceivableStatus.PARTIAL_PAID) {
            this.status = SalesInvoiceStatus.PARTIAL_PAID;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = SalesInvoiceStatus.ISSUED;
    }

    // Getter
    public Long getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public String getCustomerCode() { return customerCode; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public SalesInvoiceStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
