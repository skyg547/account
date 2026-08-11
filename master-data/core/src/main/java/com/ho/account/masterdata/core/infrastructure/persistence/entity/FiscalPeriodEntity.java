package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod.ClosingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회계기간(Fiscal Period) JPA 영속성 엔티티.
 * 
 * 🐣 [DDD & 영속성 모델 분리 교육적 주석]
 * fiscal_periods 테이블 매핑 및 비관적 락 조회를 위한 JPA 엔티티 객체입니다.
 */
@Entity
@Table(name = "fiscal_periods", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "fiscal_year", "fiscal_period" })
})
@Getter
@Setter
@NoArgsConstructor
public class FiscalPeriodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false, length = 4)
    private String fiscalYear;

    @Column(name = "fiscal_period", nullable = false, length = 2)
    private String fiscalPeriod;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClosingStatus closingStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "audit_user", length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.closingStatus == null) this.closingStatus = ClosingStatus.OPEN;
        if (this.auditUser == null) this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
