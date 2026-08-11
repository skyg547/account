package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import com.ho.account.masterdata.core.domain.model.Department.DepartmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 부서(Department) JPA 영속성 엔티티.
 * 
 * 🐣 [DDD & 영속성 모델 분리 교육적 주석]
 * departments 테이블 매핑 및 ORM 생명주기를 전담하는 객체입니다.
 */
@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_dept_code_valid", columnList = "code, valid_from, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DepartmentEntity parent;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DepartmentType type;

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
        if (this.type == null) this.type = DepartmentType.OTHER;
        if (this.validFrom == null) this.validFrom = LocalDate.now();
        if (this.validTo == null) this.validTo = LocalDate.of(9999, 12, 31);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
