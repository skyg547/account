package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ods_reconcile_hist")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsReconcileHistEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDate;

    @Column(name = "source_system", length = 255)
    private String sourceSystem;

    @Column(name = "target_system", length = 255)
    private String targetSystem;

    @Column(name = "reconcile_item", length = 255)
    private String reconcileItem;

    @Column(name = "source_amount", precision = 19, scale = 4)
    private BigDecimal sourceAmount;

    @Column(name = "target_amount", precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "diff_amount", precision = 19, scale = 4)
    private BigDecimal diffAmount;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "audit_timestamp")
    private LocalDateTime auditTimestamp;
}
