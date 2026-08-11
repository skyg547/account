package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import com.ho.account.masterdata.core.domain.model.TaxProfile.TaxType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 세무 프로파일(Tax Profile) JPA 영속성 엔티티.
 * 
 * 🐣 [DDD & 영속성 모델 분리 교육적 주석]
 * tax_profiles 테이블 매핑 전용 영속성 객체입니다.
 */
@Entity
@Table(name = "tax_profiles", indexes = {
    @Index(name = "idx_tax_code_valid", columnList = "tax_code, valid_from, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class TaxProfileEntity {

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

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "tax_account_code", length = 20)
    private String taxAccountCode;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "audit_user", length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.auditUser == null) this.auditUser = "SYSTEM";
        if (this.validFrom == null) this.validFrom = LocalDate.now();
        if (this.validTo == null) this.validTo = LocalDate.of(9999, 12, 31);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
