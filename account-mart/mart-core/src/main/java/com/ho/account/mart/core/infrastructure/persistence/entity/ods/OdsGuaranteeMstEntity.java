package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ods_guarantee_mst")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdsGuaranteeMstEntity {

    @Id
    @Column(name = "guarantee_no", length = 50)
    private String guaranteeNo;

    @Column(name = "acc_no", length = 50)
    private String accountNo;

    @Column(name = "guarantor_id", length = 50)
    private String guarantorId;

    @Column(name = "guarantor_name", length = 100)
    private String guarantorName;

    @Column(name = "guarantee_amt", precision = 19, scale = 4)
    private BigDecimal guaranteeAmount;

    @Column(name = "guarantee_ratio", precision = 5, scale = 2)
    private BigDecimal guaranteeRatio;

    @Column(name = "start_dt")
    private LocalDate startDate;

    @Column(name = "end_dt")
    private LocalDate endDate;

    @Column(name = "guarantor_rating", length = 10)
    private String guarantorRating;
}
