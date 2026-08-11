package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import com.ho.account.masterdata.core.domain.model.AccountSubject.AccountCategory;
import com.ho.account.masterdata.core.domain.model.AccountSubject.AccountType;
import com.ho.account.masterdata.core.domain.model.AccountSubject.BalanceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 계정과목(Account Subject) JPA 영속성 엔티티.
 * 
 * 🐣 [DDD & 영속성 모델 분리 교육적 주석]
 * 이 클래스는 데이터베이스 테이블(account_subjects) 구조, 인덱스, ORM 매핑 정보 및 JPA 생명주기 콜백만 담당합니다.
 * 도메인 모델(AccountSubject)과 영속성 엔티티(AccountSubjectEntity)를 분리하여,
 * DB 스키마 변경이나 JPA 매핑 어노테이션 수정이 순수 비즈니스 로직에 영향을 주지 않도록 격리합니다.
 */
@Entity
@Table(name = "account_subjects", indexes = {
    @Index(name = "idx_account_code_valid", columnList = "code, valid_from, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountSubjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AccountSubjectEntity parent;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AccountCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BalanceType balanceType;

    @Column(length = 100)
    private String reportLine;

    @Column(length = 100)
    private String regulatoryMappingCode;

    @Column(nullable = false)
    private boolean unsettled;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false)
    private boolean fixedAsset;

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
        if (this.category == null) this.category = AccountCategory.ASSETS;
        if (this.accountType == null) this.accountType = AccountType.valueOf(this.category.name());
        if (this.balanceType == null && this.category != null) {
            this.balanceType = (category == AccountCategory.LIABILITIES || category == AccountCategory.EQUITY || category == AccountCategory.REVENUE)
                    ? BalanceType.CREDIT : BalanceType.DEBIT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
