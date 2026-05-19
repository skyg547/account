package com.ho.account.tax.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 세금계산서(Tax Invoice) 엔티티 — 공식적인 매입/매출 증빙 문서를 관리합니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 세금계산서는 나라에서 인정하는 아주 중요한 '영수증'입니다. 
 * 물건을 살 때 받은 영수증(매입)과 팔 때 준 영수증(매출)을 모두 기록합니다. 
 * 이 클래스는 "물건값이 얼마인지", "세금은 얼마인지"를 기록하고, 
 * 이 둘을 더한 금액이 최종 합계와 딱 맞는지(금액 정합성)를 확인하는 역할을 합니다.
 *
 * ─────────────────────────────────────────────────
 * [도메인 설명]
 * 세금계산서 정보를 관리하며, 금액 검증 및 타 모듈(Journal Ledger)과의 독립성을 보장합니다.
 */
@Entity
@Table(name = "tax_invoices")
public class TaxInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String issueId; // 승인번호

    @Column(nullable = false)
    private String type; // SALES(매출), PURCHASE(매입)

    @Column(nullable = false)
    private LocalDate issueDate; // 작성일자

    @Column(name = "business_partner_code", length = 20, nullable = false)
    private String businessPartnerCode; // 공급받는자(매출) 또는 공급자(매입)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal supplyAmount; // 공급가액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount; // 세액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount; // 합계금액

    /**
     * 연관된 전표 ID.
     */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    protected TaxInvoice() {}

    /**
     * 세금계산서 생성을 위한 정적 팩토리 메서드.
     */
    public static TaxInvoice create(String issueId, String type, LocalDate issueDate, String bpCode,
                                   BigDecimal supply, BigDecimal tax, BigDecimal total) {
        TaxInvoice invoice = new TaxInvoice();
        invoice.issueId = issueId;
        invoice.type = type;
        invoice.issueDate = issueDate;
        invoice.businessPartnerCode = bpCode;
        invoice.supplyAmount = supply;
        invoice.taxAmount = tax;
        invoice.totalAmount = total;
        invoice.validateAmounts();
        return invoice;
    }

    public void validateAmounts() {
        if (supplyAmount == null || taxAmount == null || totalAmount == null) {
            throw new IllegalArgumentException("공급가액, 세액, 합계금액은 필수입니다.");
        }
        if (supplyAmount.add(taxAmount).compareTo(totalAmount) != 0) {
            throw new IllegalStateException("공급가액(" + supplyAmount + ")과 세액(" + taxAmount + ")의 합이 합계금액(" + totalAmount + ")과 일치하지 않습니다.");
        }
    }

    public boolean isPurchaseType() {
        return "PURCHASE".equals(this.type);
    }

    /**
     * 세금계산서 정보를 업데이트합니다.
     */
    public void updateInfo(String issueId, LocalDate issueDate, String bpCode, 
                          BigDecimal supplyAmount, BigDecimal taxAmount, BigDecimal totalAmount) {
        this.issueId = issueId;
        this.issueDate = issueDate;
        this.businessPartnerCode = bpCode;
        this.supplyAmount = supplyAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        validateAmounts();
    }

    /**
     * 회계 전표와 연결합니다.
     */
    public void linkJournalEntry(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    // Getter
    public Long getId() { return id; }
    public String getIssueId() { return issueId; }
    public String getType() { return type; }
    public LocalDate getIssueDate() { return issueDate; }
    public String getBusinessPartnerCode() { return businessPartnerCode; }
    public BigDecimal getSupplyAmount() { return supplyAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Long getJournalEntryId() { return journalEntryId; }
}
