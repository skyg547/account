package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ods_balance_hist")
@IdClass(OdsBalanceHistId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsBalanceHistEntity {
    @Id
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDate;

    @Id
    @Column(name = "account_no", nullable = false, length = 50)
    private String accountNo;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;
}
