package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.PeriodLock;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of PeriodLock; business transitions live in the domain aggregate. */
@Entity(name = "PeriodLock")
@Table(name = "period_locks")
@Getter
@Setter
public class PeriodLockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_period_id", nullable = false)
    private Long fiscalPeriodId;

    @Transient
    private String fiscalYear;

    @Transient
    private String fiscalPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PeriodLock.PeriodLockType lockType;

    @Column(length = 50)
    private String lockedBy;

    private LocalDateTime lockedAt;

    @Column(length = 1000)
    private String reason;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null)
            this.auditUser = "SYSTEM";
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
