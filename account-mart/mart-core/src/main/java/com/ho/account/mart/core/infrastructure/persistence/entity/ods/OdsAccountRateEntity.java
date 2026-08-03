package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ods_rate_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdsAccountRateEntity {

    @Id
    @Column(name = "acc_no", nullable = false, length = 50)
    private String accountNo;

    @Column(name = "rate_type", nullable = false, length = 10)
    private String rateType;

    @Column(name = "base_rate_cd", length = 20)
    private String baseRateCode;

    @Column(name = "spread", precision = 10, scale = 6)
    private BigDecimal spread;

    @Column(name = "applied_rate", precision = 10, scale = 6)
    private BigDecimal appliedRate;

    @Column(name = "int_rate_cap", precision = 10, scale = 6)
    private BigDecimal interestRateCap;

    @Column(name = "int_rate_floor", precision = 10, scale = 6)
    private BigDecimal interestRateFloor;

    @Column(name = "ref_index_cd", length = 20)
    private String refIndexCode;

    @Column(name = "payment_freq")
    private Integer paymentFreq;

    @Column(name = "rate_reset_cycle")
    private Integer rateResetCycle;

    @Column(name = "last_reset_dt")
    private LocalDate lastResetDate;

    @Column(name = "next_reset_dt")
    private LocalDate nextResetDate;
}
