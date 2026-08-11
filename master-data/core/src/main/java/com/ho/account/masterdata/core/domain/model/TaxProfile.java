package com.ho.account.masterdata.core.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 세무 프로파일(Tax Profile) 도메인 모델
 * 부가세(VAT), 원천세 등 세금 유형별 세율 및 회계 연결 정보를 관리합니다.
 * SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 세율 변동 이력을 추적합니다.
 *
 * 🐣 [DDD & Pure POJO 원칙 교육적 주석]
 * 1. Pure POJO 원칙:
 *    도메인 모델에서 JPA 기술 종속성 어노테이션을 제거하여 독립적인 pure POJO로 관리합니다.
 * 2. 도메인-영속성 모델 분리:
 *    TaxProfileEntity와 Data Mapper를 활용하여 프레임워크 결합을 방지합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TaxProfile {

    private Long id;
    private String taxCode;
    private String name;
    private TaxType taxType;

    /**
     * 세율 (예: 0.1000 for 10%)
     */
    private BigDecimal taxRate;

    /**
     * 부가세 대급금/예수금 계정 코드
     */
    private String taxAccountCode;

    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public enum TaxType {
        VAT_INPUT,  // 매입 부가세
        VAT_OUTPUT, // 매출 부가세
        WITHHOLDING, // 원천세
        ZERO_RATE,   // 영세율
        EXEMPT       // 면세
    }
}

