package com.ho.account.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [OperationalRiskLoss]
 * 운영리스크 손실 사건 기록 엔티티.
 * 내부 프로세스, 인력, 시스템 오류 등으로 인해 발생한 실제 손실 데이터를 관리합니다.
 */
@Entity
@Table(name = "operational_risk_losses")
@Getter @Setter
public class OperationalRiskLoss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate occurrenceDate; // 사건 발생일

    @Column(nullable = false)
    private LocalDateTime discoveryTime; // 사건 인지 시점

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LossEventCategory category; // 손실 사건 분류

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossLossAmount; // 총 손실 금액

    @Column(precision = 19, scale = 2)
    private BigDecimal recoveryAmount; // 회수 금액

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netLossAmount; // 순 손실 금액

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 사건 상세 설명

    @Column(length = 50)
    private String relatedModule; // 관련 모듈 (LOAN, JOURNAL, TAX 등)

    @Column(length = 30)
    private String status; // 처리 상태 (REPORTED, INVESTIGATING, CLOSED)

    public enum LossEventCategory {
        INTERNAL_FRAUD, // 내부 사기
        EXTERNAL_FRAUD, // 외부 사기
        EMPLOYMENT_PRACTICES, // 고용 관행 및 작업장 안전
        CLIENTS_PRODUCTS_BUSINESS_PRACTICES, // 고객, 상품 및 비즈니스 관행
        DAMAGE_TO_PHYSICAL_ASSETS, // 실물 자산의 손상
        BUSINESS_DISRUPTION_SYSTEM_FAILURES, // 비즈니스 중단 및 시스템 오류
        EXECUTION_DELIVERY_PROCESS_MANAGEMENT // 실행, 인도 및 프로세스 관리
    }

    /**
     * 순 손실 금액 계산
     */
    public void calculateNetLoss() {
        BigDecimal recovery = (recoveryAmount != null) ? recoveryAmount : BigDecimal.ZERO;
        this.netLossAmount = grossLossAmount.subtract(recovery);
    }
}
