package com.risk.credit.core.domain.collateral;

import com.risk.common.entity.BaseEntity;
import com.risk.credit.core.domain.exposure.CrAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * [Entity] 계좌-담보 배분 매핑 엔티티
 * 계좌와 여러 담보가 연결되거나 하나의 담보가 여러 계좌에 배분되는 N:M 관계를 관리합니다.
 *
 * [초보자를 위한 개념 설명]
 * 신용리스크 관리에서는 하나의 담보가 여러 여신(계좌)을 보충하는 경우가 많습니다. 
 * 이때 "금액을 어떤 순서로 얼마만큼 나눌 것인가"가 매우 중요하며, 
 * 이 엔티티는 특정 담보가 특정 계좌에 '얼마나 배분(Allocation)' 되었는지 내역을 기록합니다.
 */
@Entity
@Table(name = "cr_account_collaterals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrAccountCollateral extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결 계좌 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private CrAccount account;

    /** 연결 담보 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collateral_id", nullable = false)
    private CrCollateral collateral;

    /** 해당 계좌에 할당된 담보 가액 금액 (Allocation Amount) */
    @Column(name = "allocation_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocationAmount;

    /** 우선 순위 (배분 순서) */
    @Column(name = "priority")
    private Integer priority;
}
