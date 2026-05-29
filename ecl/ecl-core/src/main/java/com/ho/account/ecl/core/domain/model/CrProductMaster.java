package com.ho.account.ecl.core.domain.model;

import com.ho.account.shared.finance.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * [Entity] 상품별 대손충당금 산출 파라미터 마스터 엔티티.
 */
@Entity
@Table(name = "cr_product_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrProductMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상품 코드 */
    @Column(name = "product_code", nullable = false, unique = true, length = 20)
    private String productCode;

    /** 상품명 (예: 신용대출, 주택담보대출, 한도약정 등) */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** 신용전환계수 (CCF) - 미사용 한도 등 난외(Off-Balance) 자산을 EAD로 환산할 때 곱하는 비율 */
    @Column(name = "ccf_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal ccfRate;

    /** 상품 상세 설명 */
    @Column(name = "description", length = 500)
    private String description;
}
