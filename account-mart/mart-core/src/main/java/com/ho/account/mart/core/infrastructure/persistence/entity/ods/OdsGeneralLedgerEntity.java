package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ods_general_ledger")
@IdClass(OdsGeneralLedgerId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsGeneralLedgerEntity {
    @Id
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDate;

    @Id
    @Column(name = "gl_code", nullable = false, length = 20)
    private String glCode;

    @Id
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance;

    @Id
    @Column(name = "branch_cd", nullable = false, length = 10)
    private String branchCode;
}
