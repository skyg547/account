package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회계기간(Fiscal Period) 엔티티
 * 특정 연도의 월별 마감 상태를 관리합니다.
 */
@Entity
@Table(name = "fiscal_periods", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "fiscal_year", "fiscal_period" })
})
@Getter
@Setter
@NoArgsConstructor
public class FiscalPeriod {

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

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum ClosingStatus {
        OPEN, CLOSED, PERMANENTLY_CLOSED
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.closingStatus == null)
            this.closingStatus = ClosingStatus.OPEN;
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Deprecated
    public String getFiscalYearAndPeriod() {
        return fiscalYear + "-" + fiscalPeriod;
    }
}
