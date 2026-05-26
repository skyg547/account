package com.risk.credit.core.domain.model;

import com.risk.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * [Entity] 상품별 리스크 규제 파라미터 마스터 엔티티
 * 바젤 III/IV 기준의 신용전환계수(CCF) 및 표준방법 위험가중치(RW) 등 상품별 기초 리스크 속성을 관리합니다.
 *
 * [초보자를 위한 개념 설명]
 * 은행의 모든 상품은 성격에 따라 리스크가 다릅니다. 
 * 예를 들어, '마이너스 통장' 같은 상품은 지금 당장 대출 잔액이 없더라도 
 * 나중에 고객이 돈을 인출할 수 있는 잠재적인 위험이 있습니다. 이때 사용하는 것이 CCF(신용전환계수)입니다. 
 * 이 마스터 테이블은 개별 상품이 규제상 어떤 위험 가중치와 전환 계수를 가져야 하는지 정의합니다.
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

    /** 표준방법 위험가중치 (Standard RW) - 규제 당국이 정한 자산별 위험 비율 */
    @Column(name = "standard_rw", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal standardRw = BigDecimal.ZERO;

    /** 상품 상세 설명 */
    @Column(name = "description", length = 500)
    private String description;
}
