package com.ho.account.reconciliation.domain;

import com.ho.account.closing.domain.ClosingPeriod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate reconciliationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationType reconciliationType;

    // Optional: Link to a ClosingPeriod if reconciliation is part of a closing process
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closing_period_id")
    private ClosingPeriod closingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    @Column(nullable = false)
    private Long totalCountSource;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountSource;

    @Column(nullable = false)
    private Long totalCountTarget;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountTarget;

    @Column(nullable = false)
    private Long varianceCount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal varianceAmount;

    private String runBy;
    private LocalDateTime runAt;

    public ReconciliationResult() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getReconciliationDate() { return reconciliationDate; }
    public void setReconciliationDate(LocalDate reconciliationDate) { this.reconciliationDate = reconciliationDate; }

    public ReconciliationType getReconciliationType() { return reconciliationType; }
    public void setReconciliationType(ReconciliationType reconciliationType) { this.reconciliationType = reconciliationType; }

    public ClosingPeriod getClosingPeriod() { return closingPeriod; }
    public void setClosingPeriod(ClosingPeriod closingPeriod) { this.closingPeriod = closingPeriod; }

    public ReconciliationStatus getStatus() { return status; }
    public void setStatus(ReconciliationStatus status) { this.status = status; }

    public Long getTotalCountSource() { return totalCountSource; }
    public void setTotalCountSource(Long totalCountSource) { this.totalCountSource = totalCountSource; }

    public BigDecimal getTotalAmountSource() { return totalAmountSource; }
    public void setTotalAmountSource(BigDecimal totalAmountSource) { this.totalAmountSource = totalAmountSource; }

    public Long getTotalCountTarget() { return totalCountTarget; }
    public void setTotalCountTarget(Long totalCountTarget) { this.totalCountTarget = totalCountTarget; }

    public BigDecimal getTotalAmountTarget() { return totalAmountTarget; }
    public void setTotalAmountTarget(BigDecimal totalAmountTarget) { this.totalAmountTarget = totalAmountTarget; }

    public Long getVarianceCount() { return varianceCount; }
    public void setVarianceCount(Long varianceCount) { this.varianceCount = varianceCount; }

    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public void setVarianceAmount(BigDecimal varianceAmount) { this.varianceAmount = varianceAmount; }

    public String getRunBy() { return runBy; }
    public void setRunBy(String runBy) { this.runBy = runBy; }

    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime runAt) { this.runAt = runAt; }
}
