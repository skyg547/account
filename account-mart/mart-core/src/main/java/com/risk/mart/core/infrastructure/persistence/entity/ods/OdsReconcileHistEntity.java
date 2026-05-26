package com.risk.mart.core.infrastructure.persistence.entity.ods;

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
    private LocalDate baseDate;
    private String sourceSystem;
    private String targetSystem;
    private String reconcileItem;
    private BigDecimal sourceAmount;
    private BigDecimal targetAmount;
    private BigDecimal diffAmount;
    private String status;
    private LocalDateTime auditTimestamp;
}
