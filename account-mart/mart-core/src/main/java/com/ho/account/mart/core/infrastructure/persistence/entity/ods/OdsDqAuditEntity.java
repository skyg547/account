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
    private LocalDate baseDate;
    private String accountNo;
    private String auditType;
    private String auditMessage;
    private String severity;
    private LocalDateTime auditTimestamp;
}
