package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ods_dq_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsDqAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "audit_type", length = 30)
    private String auditType;

    @Column(name = "audit_message", columnDefinition = "TEXT")
    private String auditMessage;

    @Column(name = "severity", length = 10)
    private String severity;

    @Column(name = "audit_timestamp")
    private LocalDateTime auditTimestamp;
}
