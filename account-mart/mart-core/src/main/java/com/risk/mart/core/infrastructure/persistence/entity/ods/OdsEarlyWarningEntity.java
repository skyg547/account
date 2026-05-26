package com.risk.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "ods_early_warning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(OdsEarlyWarningId.class)
public class OdsEarlyWarningEntity {

    @Id
    @Column(name = "base_dt")
    private LocalDate baseDate;

    @Id
    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "warning_level", length = 10)
    private String warningLevel;

    @Column(name = "warning_reason")
    private String warningReason;

    @Column(name = "warning_score")
    private Integer warningScore;
}
