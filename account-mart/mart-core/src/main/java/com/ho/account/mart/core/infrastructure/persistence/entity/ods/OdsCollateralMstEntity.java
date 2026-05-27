package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ods_coll_mst")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsCollateralMstEntity {
    @Id
    @Column(name = "collateral_no", length = 50)
    private String collateralNo;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "collateral_type", length = 20)
    private String collateralType;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "appraisal_amount", precision = 19, scale = 4)
    private BigDecimal appraisalAmount;

    @Column(name = "pledge_amount", precision = 19, scale = 4)
    private BigDecimal pledgeAmount;

    @Column(name = "appraisal_date")
    private LocalDate appraisalDate;

    @Column(name = "is_active")
    private Boolean isActive;
}
