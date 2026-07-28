package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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
    @Setter(AccessLevel.NONE)
    private ClosingStatus closingStatus;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum ClosingStatus {
        OPEN, CLOSED, PERMANENTLY_CLOSED
    }

    /**
     * 회계기간 상태를 도메인 규칙과 감사 사용자 검증을 거쳐 변경합니다.
     * 영구 마감은 되돌릴 수 없고, 열린 기간은 일반 마감을 거치지 않고 영구 마감할 수 없습니다.
     */
    public void changeClosingStatus(ClosingStatus nextStatus, String changedBy) {
        ClosingStatus next = Objects.requireNonNull(nextStatus, "Closing status is required.");
        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalArgumentException("Closing status audit user is required.");
        }

        ClosingStatus current = closingStatus == null ? ClosingStatus.OPEN : closingStatus;
        if (current == ClosingStatus.PERMANENTLY_CLOSED && next != current) {
            throw new IllegalStateException("Permanently closed fiscal period cannot be reopened or changed.");
        }
        if (current == ClosingStatus.OPEN && next == ClosingStatus.PERMANENTLY_CLOSED) {
            throw new IllegalStateException("Fiscal period must be closed before permanent closing.");
        }

        this.closingStatus = next;
        this.auditUser = changedBy.trim();
        this.updatedAt = LocalDateTime.now();
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
