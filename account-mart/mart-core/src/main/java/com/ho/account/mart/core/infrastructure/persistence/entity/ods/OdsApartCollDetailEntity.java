package com.ho.account.mart.core.infrastructure.persistence.entity.ods;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ods_apart_coll_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdsApartCollDetailEntity {

    @Id
    @Column(name = "coll_id", nullable = false, length = 50)
    private String collateralId;

    @Column(name = "district_cd", length = 10)
    private String districtCode;

    @Column(name = "kb_market_price", precision = 19, scale = 4)
    private BigDecimal kbMarketPrice;

    @Column(name = "house_type", length = 20)
    private String houseType;

    @Column(name = "exclusive_area", precision = 10, scale = 2)
    private BigDecimal exclusiveArea;

    @Column(name = "floor_no")
    private Integer floorNo;

    @Column(name = "is_speculative_area")
    @Builder.Default
    private Boolean isSpeculativeArea = false;
}
