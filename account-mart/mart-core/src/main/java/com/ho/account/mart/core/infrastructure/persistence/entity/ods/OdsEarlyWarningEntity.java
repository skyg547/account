package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

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
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDate;

    @Id
    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(name = "warning_level", length = 10)
    private String warningLevel;

    @Column(name = "warning_reason", length = 255)
    private String warningReason;

    @Column(name = "warning_score")
    private Integer warningScore;
}
