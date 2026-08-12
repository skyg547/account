package com.ho.account.expenditure.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [PurchaseInvoice] 도메인 엔티티 (Pure Java POJO).
 * 공급업체로부터 수취한 매입 인보이스 정보를 관리합니다.
 * 타 모듈(Master Data, Journal Ledger)과는 ID/Code 기반으로 참조하여 결합도를 낮춥니다.
 * 
 * 🐣 [Hexagonal Architecture & Pure Java POJO 교육적 주석]
 * 1. 도메인 계층의 기술/영속성 프레임워크 독립성 (Hexagonal Architecture Core):
 *    본 도메인 클래스는 @Entity, @Column, @Id 등 JPA 기술 어노테이션을 전혀 포함하지 않는 Pure Java POJO입니다.
 *    이를 통해 기술 프레임워크(Hibernate, Spring Data)의 변경이나 DB 스키마 수정에 영향을 받지 않고
 *    순수한 비즈니스 로직 및 회계 규칙에 집중할 수 있으며, 가볍고 빠른 단위 테스트(Unit Test)를 가능하게 합니다.
 * 
 * 2. Data Mapper 패턴을 통한 영속성 분리:
 *    영속성(Persistence) 처리는 Infrastructure 계층의 PurchaseInvoiceJpaEntity 및 PurchaseInvoiceMapper가 담당하여
 *    도메인 모델과 데이터베이스 테이블 구조 간의 결합을 완벽하게 제거합니다.
 */
public class PurchaseInvoice {

    private Long id;
    private String invoiceNo;
    private String vendorCode;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private PurchaseInvoiceStatus status = PurchaseInvoiceStatus.RECEIVED;
    private Long journalEntryId;
    private String description;
    private String createdBy = "SYSTEM";
    private LocalDateTime createdAt = LocalDateTime.now();

    public PurchaseInvoice() {
    }

    // Getter 및 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

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

    public PurchaseInvoiceStatus getStatus() { return status; }
    public void setStatus(PurchaseInvoiceStatus status) { this.status = status; }

    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
