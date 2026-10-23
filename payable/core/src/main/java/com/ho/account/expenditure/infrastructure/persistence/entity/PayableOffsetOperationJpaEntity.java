package com.ho.account.expenditure.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payable_offset_operations")
@IdClass(PayableOffsetOperationId.class)
public class PayableOffsetOperationJpaEntity {
    @Id
    @Column(name = "operation_id", length = 60)
    private String operationId;

    @Id
    @Column(name = "payable_id", nullable = false)
    private Long payableId;

    @Column(name = "advance_payment_id", nullable = false)
    private Long advancePaymentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "accounting_date", nullable = false)
    private LocalDate accountingDate;

    @Column(name = "journal_entry_id", nullable = false)
    private Long journalEntryId;

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }
    public Long getAdvancePaymentId() { return advancePaymentId; }
    public void setAdvancePaymentId(Long advancePaymentId) { this.advancePaymentId = advancePaymentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getAccountingDate() { return accountingDate; }
    public void setAccountingDate(LocalDate accountingDate) { this.accountingDate = accountingDate; }
    public Long getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(Long journalEntryId) { this.journalEntryId = journalEntryId; }
}
