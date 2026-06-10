package com.ho.account.reconciliation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Deep reconciliation의 단계별 집계 결과.
 *
 * <p>초보자 가이드: 하나의 대사 실행 성적표({@link ReconciliationRun}) 안에서 원천, 인터페이스,
 * 전표, 원장 금액이 어디서 달라졌는지 추적하기 위한 중간 체크포인트입니다.
 */
@Entity
@Table(name = "reconciliation_stage_results")
public class ReconciliationStageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRun reconciliationRun;

    @Column(name = "stage_code", nullable = false, length = 20)
    private String stageCode;

    @Column(name = "total_count", nullable = false)
    private Long totalCount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "audit_user", nullable = false, length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (auditUser == null) auditUser = "SYSTEM";
    }

    public Long getId() { return id; }
    public ReconciliationRun getReconciliationRun() { return reconciliationRun; }
    public void setReconciliationRun(ReconciliationRun reconciliationRun) { this.reconciliationRun = reconciliationRun; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAuditUser() { return auditUser; }
    public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
}
