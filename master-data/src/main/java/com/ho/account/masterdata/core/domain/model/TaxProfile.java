package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 세무 프로파일(Tax Profile) 엔티티
 * 부가세(VAT), 원천세 등 세금 유형별 세율 및 회계 연결 정보를 관리합니다.
 * SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 세율 변동 이력을 추적합니다.
 *
 * <p> 현재 이 엔티티에는 repository, application use case, 승인 applier, 소비 계약이 없습니다.
 * 완료 조건은 Tax 모듈과 소유권을 먼저 결정한 뒤, Master Data 소유라면 SCD2 포트/어댑터/승인 전략과
 * 기준일 조회 통합 테스트를 모두 구현하고, Tax 소유라면 이 중복 엔티티를 이관·제거하는 것입니다.</p>
 */
@Entity
@Table(name = "tax_profiles", indexes = {
    @Index(name = "idx_tax_code_valid", columnList = "tax_code, valid_from, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class TaxProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_code", nullable = false, length = 20)
    private String taxCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaxType taxType;

    /**
     * 세율 (예: 0.1000 for 10%)
     */
    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    /**
     * 부가세 대급금/예수금 계정 코드 (ID 기반 연관관계 대신 코드로 관리하여 모듈 독립성 확보 가능하나 마스터 내에서는 객체 참조도 가능)
     * 여기서는 단순함을 위해 코드로 관리.
     */
    @Column(name = "tax_account_code", length = 20)
    private String taxAccountCode;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    public enum TaxType {
        VAT_INPUT,  // 매입 부가세
        VAT_OUTPUT, // 매출 부가세
        WITHHOLDING, // 원천세
        ZERO_RATE,   // 영세율
        EXEMPT       // 면세
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) this.auditUser = "SYSTEM";
        if (this.validFrom == null) this.validFrom = LocalDate.now();
        if (this.validTo == null) this.validTo = LocalDate.of(9999, 12, 31);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
