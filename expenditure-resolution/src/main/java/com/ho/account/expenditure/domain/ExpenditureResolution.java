package com.ho.account.expenditure.domain;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
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
    @JoinColumn(name = "dept_code", referencedColumnName = "code")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_code")
    private AccountSubject paymentAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExpenditureResolutionStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    // 由ъ뒪 怨꾩빟 ?곌껐 (?좏깮)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_contract_id")
    private LeaseContract leaseContract;

    @Column(name = "tax_invoice_id")
    private Long taxInvoiceId;

    @OneToMany(mappedBy = "expenditureResolution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenditureDetail> details = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = ExpenditureResolutionStatus.DRAFT;
    }

    // ?곌?愿怨??몄쓽 硫붿꽌??
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

    // 도메인 상태 전이 메서드
    public void requestApproval() {
        if (this.status != ExpenditureResolutionStatus.DRAFT
                && this.status != ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("DRAFT 또는 REJECTED 상태의 결의서만 승인 요청할 수 있습니다.");
        }
        this.status = ExpenditureResolutionStatus.REQUESTED;
    }

    public void approve(JournalEntry journalEntry) {
        if (this.status != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("REQUESTED 상태의 결의서만 승인할 수 있습니다.");
        }
        this.status = ExpenditureResolutionStatus.APPROVED;
        this.journalEntry = journalEntry;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        if (this.status != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("REQUESTED 상태의 결의서만 반려할 수 있습니다.");
        }
        this.status = ExpenditureResolutionStatus.REJECTED;
        this.rejectionReason = reason;
    }

    // Getter / Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResolutionNo() {
        return resolutionNo;
    }

    public void setResolutionNo(String resolutionNo) {
        this.resolutionNo = resolutionNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(LocalDate resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public AccountSubject getPaymentAccount() {
        return paymentAccount;
    }

    public void setPaymentAccount(AccountSubject paymentAccount) {
        this.paymentAccount = paymentAccount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ExpenditureResolutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenditureResolutionStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public LeaseContract getLeaseContract() {
        return leaseContract;
    }

    public void setLeaseContract(LeaseContract leaseContract) {
        this.leaseContract = leaseContract;
    }

    public Long getTaxInvoiceId() {
        return taxInvoiceId;
    }

    public void setTaxInvoiceId(Long taxInvoiceId) {
        this.taxInvoiceId = taxInvoiceId;
    }

    public List<ExpenditureDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ExpenditureDetail> details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
