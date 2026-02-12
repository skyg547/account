package com.ho.account.expenditure.domain;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.tax.domain.TaxInvoice; // Import TaxInvoice
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenditure_resolutions")
public class ExpenditureResolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String resolutionNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDate resolutionDate;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_code", referencedColumnName = "deptCode")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_code")
    private AccountSubject paymentAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 20)
    private String status;

    @Column(length = 500)
    private String rejectionReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    // 리스 계약 연결 (선택)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id")
    private LeaseContract leaseContract;

    @ManyToOne(fetch = FetchType.LAZY) // Added ManyToOne relationship to TaxInvoice
    @JoinColumn(name = "tax_invoice_id") // Nullable by default
    private TaxInvoice taxInvoice;

    @OneToMany(mappedBy = "expenditureResolution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenditureDetail> details = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
    }

    // 연관관계 편의 메서드
    public void addDetail(ExpenditureDetail detail) {
        details.add(detail);
        detail.setExpenditureResolution(this);
        calculateTotalAmount();
    }

    public void calculateTotalAmount() {
        this.totalAmount = details.stream()
                .map(ExpenditureDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResolutionNo() { return resolutionNo; }
    public void setResolutionNo(String resolutionNo) { this.resolutionNo = resolutionNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getResolutionDate() { return resolutionDate; }
    public void setResolutionDate(LocalDate resolutionDate) { this.resolutionDate = resolutionDate; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public AccountSubject getPaymentAccount() { return paymentAccount; }
    public void setPaymentAccount(AccountSubject paymentAccount) { this.paymentAccount = paymentAccount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public JournalEntry getJournalEntry() { return journalEntry; }
    public void setJournalEntry(JournalEntry journalEntry) { this.journalEntry = journalEntry; }

    public LeaseContract getLeaseContract() { return leaseContract; }
    public void setLeaseContract(LeaseContract leaseContract) { this.leaseContract = leaseContract; }

    public TaxInvoice getTaxInvoice() { return taxInvoice; }
    public void setTaxInvoice(TaxInvoice taxInvoice) { this.taxInvoice = taxInvoice; }

    public List<ExpenditureDetail> getDetails() { return details; }
    public void setDetails(List<ExpenditureDetail> details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
