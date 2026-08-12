package com.ho.account.receivable.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * [SalesInvoice] 도메인 엔티티 (Pure Java POJO).
 * 고객에게 발행된 매출 및 청구 정보를 관리하며, 대금 수금 상태와 연령(Aging) 분석 기능을 제공합니다.
 * 
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 1. 도메인 계층의 기술/영속성 프레임워크 독립성 (Hexagonal Architecture Core):
 *    도메인 모델 객체(SalesInvoice)는 JPA 어노테이션(@Entity, @Table, @Id 등)이 전면 제거된 Pure Java POJO입니다.
 *    특정 DB나 ORM 기술 프레임워크에 종속되지 않고 매출 청구 및 Aging 분석 비즈니스 규칙을 프레임워크 독립적으로 유지합니다.
 * 
 * 2. Data Mapper 패턴의 아키텍처적 이점:
 *    DB 테이블 매핑은 SalesInvoiceJpaEntity 및 SalesInvoiceMapper가 전담하여 도메인 모델과 영속성 구조를 완전하게 격리합니다.
 */
public class SalesInvoice {

    private Long id;
    private String invoiceNo;
    private String customerCode;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private SalesInvoiceStatus status = SalesInvoiceStatus.ISSUED;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();

    public SalesInvoice() {}

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

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public SalesInvoiceStatus getStatus() { return status; }
    public void setStatus(SalesInvoiceStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
