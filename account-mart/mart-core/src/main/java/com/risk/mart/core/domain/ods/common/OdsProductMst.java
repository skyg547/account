package com.risk.mart.core.domain.ods.common;

import lombok.*;

/**
 * [순수 도메인 모델] 상품 마스터 (Product Master)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OdsProductMst {
    private String productCode;
    private String productName;
    private String productType;
    private String assetLiabilityType;
    private Boolean isActive;
}
