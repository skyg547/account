package com.ho.account.expenditure.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [ExpenditureResolution] 도메인 엔티티.
 * 지출 결의서 정보를 관리하며, 승인 프로세스 및 타 모듈과의 독립성을 보장합니다.
 */
@Entity
@Table(name = "expenditure_resolutions")
public class ExpenditureResolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String resolutionNo; // 결의 번호

    @Column(nullable = false, length = 200)
    private String title; // 결의 제목

    @Column(nullable = false)
    private LocalDate resolutionDate; // 결의 일자

    @Column(nullable = false)
    private LocalDate paymentDate; // 지급 예정일

    /**
     * 귀속 부서 코드 (ID 참조로 변경하여 모듈 간 결합도 제거)
     */
    @Column(name = "dept_code", length = 20)
    private String deptCode;

    /**
     * 지급 계정 코드
     */
    @Column(name = "payment_account_code", length = 20)
    private String paymentAccountCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExpenditureResolutionStatus status;

    @Column(length = 500)
    private String rejectionReason; // 반려 사유

    /**
     * 연관된 전표 ID (ID 참조)
     */
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    /**
     * 연관된 리스 계약 ID (ID 참조)
     */
    @Column(name = "lease_contract_id")
    private Long leaseContractId;

    /**
     * 연관된 세금계산서 ID (ID 참조)
     */
    @Column(name = "tax_invoice_id")
    private Long taxInvoiceId;

    @OneToMany(mappedBy = "expenditureResolution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenditureDetail> details = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String createdBy;

    protected ExpenditureResolution() {}

    /**
     * 지출 결의서 생성을 위한 정적 팩토리 메서드.
     */
    public static ExpenditureResolution create(String resolutionNo, String title, LocalDate resolutionDate, 
                                              LocalDate paymentDate, String deptCode, String accountCode, String createdBy) {
        ExpenditureResolution resolution = new ExpenditureResolution();
        resolution.resolutionNo = resolutionNo;
        resolution.title = title;
        resolution.resolutionDate = resolutionDate;
        resolution.paymentDate = paymentDate;
        resolution.deptCode = deptCode;
        resolution.paymentAccountCode = accountCode;
        resolution.createdBy = createdBy;
        resolution.status = ExpenditureResolutionStatus.DRAFT;
        return resolution;
    }

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

    /**
     * 승인을 요청합니다.
     */
    public void requestApproval() {
        if (this.status != ExpenditureResolutionStatus.DRAFT
                && this.status != ExpenditureResolutionStatus.REJECTED) {
            throw new IllegalStateException("DRAFT 또는 REJECTED 상태의 결의서만 승인 요청할 수 있습니다.");
        }
        this.status = ExpenditureResolutionStatus.REQUESTED;
    }

    /**
     * 결의서를 승인하고 회계 전표와 연결합니다.
     */
    public void approve(Long journalEntryId) {
        if (this.status != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("REQUESTED 상태의 결의서만 승인할 수 있습니다.");
        }
        this.status = ExpenditureResolutionStatus.APPROVED;
        this.journalEntryId = journalEntryId;
        this.rejectionReason = null;
    }

    /**
     * 결의서를 반려합니다.
     */
    public void reject(String reason) {
        if (this.status != ExpenditureResolutionStatus.REQUESTED) {
            throw new IllegalStateException("REQUESTED 상태의 결의서만 반려할 수 있습니다.");
        }
        this.status = ExpenditureResolutionStatus.REJECTED;
        this.rejectionReason = reason;
    }

    /**
     * 지출 결의서 정보를 업데이트합니다.
     */
    public void updateInfo(String title, LocalDate resolutionDate, LocalDate paymentDate, String deptCode, String accountCode) {
        this.title = title;
        this.resolutionDate = resolutionDate;
        this.paymentDate = paymentDate;
        this.deptCode = deptCode;
        this.paymentAccountCode = accountCode;
    }

    public void setResolutionNo(String resolutionNo) {
        this.resolutionNo = resolutionNo;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = ExpenditureResolutionStatus.DRAFT;
    }

    // Getter
    public Long getId() { return id; }
    public String getResolutionNo() { return resolutionNo; }
    public String getTitle() { return title; }
    public LocalDate getResolutionDate() { return resolutionDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getDeptCode() { return deptCode; }
    public String getPaymentAccountCode() { return paymentAccountCode; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public ExpenditureResolutionStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Long getJournalEntryId() { return journalEntryId; }
    public Long getLeaseContractId() { return leaseContractId; }
    public Long getTaxInvoiceId() { return taxInvoiceId; }
    public List<ExpenditureDetail> getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }

    // Setter (필요한 경우에만 제한적으로 제공하거나 도메인 메서드 사용 지향)
    public void setLeaseContractId(Long leaseContractId) { this.leaseContractId = leaseContractId; }
    public void setTaxInvoiceId(Long taxInvoiceId) { this.taxInvoiceId = taxInvoiceId; }
}
