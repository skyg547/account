package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ProvisionBatch;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Database representation of ProvisionBatch; business transitions live in the domain aggregate. */
@Entity(name = "ProvisionBatch")
@Table(name = "provision_batches")
@Getter
@Setter
public class ProvisionBatchEntity {

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
    private ProvisionBatch.ProvisionType provisionType;

    @Column(nullable = false)
    private LocalDateTime runDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProvisionBatch.ProvisionBatchStatus status;

    @Column(name = "generated_journal_entry_id")
    private Long generatedJournalEntryId;

    @Column(length = 200)
    private String reportLink;

    @Column(length = 50)
    private String runBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ProvisionBatch.ProvisionBatchStatus.RUNNING;
        }
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
